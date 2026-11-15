package statementimport

import (
	"bytes"
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"strconv"
	"time"

	"github.com/DarkAbhi/life-backend/internal/statementimport/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type Handler struct {
	svc       *Service
	sessionID func(*http.Request) (int64, error)
	uploads   chan struct{}
}

func NewHandler(pool *pgxpool.Pool, sessionID func(*http.Request) (int64, error), extractorURL string) *Handler {
	client := &http.Client{
		CheckRedirect: func(_ *http.Request, _ []*http.Request) error { return http.ErrUseLastResponse },
	}
	return &Handler{
		svc:       &Service{pool: pool, extractor: extractor{url: extractorURL, client: client}},
		sessionID: sessionID,
		uploads:   make(chan struct{}, 2),
	}
}

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Route("/horizon/statement-imports", func(r chi.Router) {
		r.Post("/", h.authenticated(h.upload))
		r.Get("/{id}", h.authenticated(h.read))
		r.Get("/{id}/rows", h.authenticated(h.read))
		r.Get("/{id}/structure", h.authenticated(h.read))
		r.Patch("/{id}/rows", h.authenticated(h.mutate))
		r.Post("/{id}/commit", h.authenticated(h.mutate))
		r.Delete("/{id}", h.authenticated(h.mutate))
	})
}

func (h *Handler) Cleanup(ctx context.Context) {
	ticker := time.NewTicker(15 * time.Minute)
	defer ticker.Stop()
	for {
		work, cancel := context.WithTimeout(ctx, 10*time.Second)
		if err := query.New(h.svc.pool).ExpireBatches(work); err != nil && ctx.Err() == nil {
			slog.ErrorContext(ctx, "statement import preview cleanup failed")
		}
		cancel()
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
	}
}

func (h *Handler) authenticated(next func(http.ResponseWriter, *http.Request, int64)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-Control", "no-store")
		user, err := h.sessionID(r)
		switch {
		case errors.Is(err, sql.ErrNoRows):
			webutil.Unauthorized(w, "session is invalid or expired")
		case err != nil:
			writeFailure(w, r, internal, "")
		default:
			next(w, r, user)
		}
	}
}

func writeFailure(w http.ResponseWriter, r *http.Request, fail *failure, batchID string) {
	body := map[string]string{"error": fail.message, "code": fail.code, "request_id": middleware.GetReqID(r.Context())}
	if batchID != "" {
		body["batch_id"] = batchID
	}
	webutil.WriteJSON(w, fail.status, body)
}

func readPDF(w http.ResponseWriter, r *http.Request) ([]byte, int64, *failure) {
	r.Body = http.MaxBytesReader(w, r.Body, maxPDFSize+(128<<10))
	reader, err := r.MultipartReader()
	if err != nil {
		return nil, 0, badRequest
	}
	var pdf []byte
	var account int64
	for {
		part, err := reader.NextPart()
		if errors.Is(err, io.EOF) {
			break
		}
		if err != nil {
			var size *http.MaxBytesError
			if errors.As(err, &size) {
				return nil, 0, tooLarge
			}
			return nil, 0, badRequest
		}
		switch part.FormName() {
		case "file":
			if pdf != nil {
				return nil, 0, badRequest
			}
			pdf, err = io.ReadAll(io.LimitReader(part, maxPDFSize+1))
			var size *http.MaxBytesError
			if len(pdf) > maxPDFSize || errors.As(err, &size) {
				return nil, 0, tooLarge
			}
			if err != nil || !bytes.HasPrefix(pdf, []byte("%PDF-")) {
				return nil, 0, notPDF
			}
		case "account_id":
			raw, readErr := io.ReadAll(io.LimitReader(part, 21))
			id, parseErr := strconv.ParseInt(string(raw), 10, 64)
			if account != 0 || readErr != nil || parseErr != nil || len(raw) > 19 || id <= 0 {
				return nil, 0, badRequest
			}
			account = id
		default:
			return nil, 0, badRequest
		}
	}
	if pdf == nil || account <= 0 {
		return nil, 0, badRequest
	}
	return pdf, account, nil
}

func (h *Handler) upload(w http.ResponseWriter, r *http.Request, user int64) {
	select {
	case h.uploads <- struct{}{}:
		defer func() { <-h.uploads }()
	default:
		writeFailure(w, r, busy, "")
		return
	}
	controller := http.NewResponseController(w)
	_ = controller.SetReadDeadline(time.Now().Add(60 * time.Second))
	pdf, account, fail := readPDF(w, r)
	_ = controller.SetReadDeadline(time.Time{})
	if fail != nil {
		writeFailure(w, r, fail, "")
		return
	}
	b, fail := h.svc.upload(r.Context(), user, account, pdf)
	if fail != nil {
		statementAttempts.WithLabelValues("upload", fail.code).Inc()
		writeFailure(w, r, fail, b.ID.String())
		return
	}
	statementAttempts.WithLabelValues("upload", "ready").Inc()
	rows, fail := getPage(r.Context(), query.New(h.svc.pool), b, 1, 100)
	if fail != nil {
		writeFailure(w, r, fail, b.ID.String())
		return
	}
	webutil.WriteJSON(w, http.StatusCreated, struct {
		batchSummary
		previewPage
	}{summary(b), rows})
}

func (h *Handler) read(w http.ResponseWriter, r *http.Request, user int64) {
	id, err := parseBatchID(chi.URLParam(r, "id"))
	if err != nil {
		writeFailure(w, r, badRequest, "")
		return
	}
	ctx := r.Context()
	q := query.New(h.svc.pool)
	b, err := q.GetBatch(ctx, query.GetBatchParams{ID: id, UserID: user})
	if err != nil {
		writeFailure(w, r, dbFailure(ctx, err), "")
		return
	}
	if b.Status == "expired" || (b.Status != "committed" && !b.ExpiresAt.Time.After(time.Now())) {
		writeFailure(w, r, expired, id.String())
		return
	}
	page, pageSize := parsePage(r)
	switch chi.RouteContext(ctx).RoutePattern() {
	case "/api/horizon/statement-imports/{id}/rows", "/horizon/statement-imports/{id}/rows":
		if b.Status != "ready" && b.Status != "committed" {
			writeFailure(w, r, notReady, id.String())
			return
		}
		res, fail := getPage(ctx, q, b, page, pageSize)
		if fail != nil {
			writeFailure(w, r, fail, id.String())
			return
		}
		webutil.WriteJSON(w, http.StatusOK, res)
	case "/api/horizon/statement-imports/{id}/structure", "/horizon/statement-imports/{id}/structure":
		if b.Status != "unsupported" {
			writeFailure(w, r, notFound, id.String())
			return
		}
		raw, err := q.GetStructure(ctx, id)
		if err != nil {
			writeFailure(w, r, dbFailure(ctx, err), id.String())
			return
		}
		var structure struct {
			Pages []json.RawMessage `json:"pages"`
		}
		if json.Unmarshal(raw, &structure) != nil || int(page) > len(structure.Pages) {
			writeFailure(w, r, notFound, id.String())
			return
		}
		webutil.WriteJSON(w, http.StatusOK, map[string]any{"page_count": len(structure.Pages), "page": structure.Pages[page-1]})
	default:
		webutil.WriteJSON(w, http.StatusOK, summary(b))
	}
}

func parsePage(r *http.Request) (int32, int32) {
	size := int64(100)
	if n, err := strconv.ParseInt(r.URL.Query().Get("page_size"), 10, 32); err == nil && n > 0 {
		size = min(n, 100)
	}
	page := int64(1)
	if n, err := strconv.ParseInt(r.URL.Query().Get("page"), 10, 32); err == nil && n > 0 {
		page = min(n, 10_001)
	}
	return int32(page), int32(size)
}

func getPage(ctx context.Context, q *query.Queries, b query.StatementImportBatch, page, pageSize int32) (previewPage, *failure) {
	rows, err := q.ListPage(ctx, query.ListPageParams{BatchID: b.ID, Limit: pageSize, Offset: (page - 1) * pageSize})
	if err != nil {
		return previewPage{}, dbFailure(ctx, err)
	}
	preview, _, err := rowsForReview(rows)
	if err != nil {
		return previewPage{}, dbFailure(ctx, err)
	}
	return previewPage{Rows: preview, Page: page, PageSize: pageSize, Total: b.ParsedCount}, nil
}

func (h *Handler) mutate(w http.ResponseWriter, r *http.Request, user int64) {
	ctx := r.Context()
	id, err := parseBatchID(chi.URLParam(r, "id"))
	var input editInput
	if err != nil || (r.Method != http.MethodDelete && !readEdit(w, r, &input)) {
		writeFailure(w, r, badRequest, "")
		return
	}
	tx, err := h.svc.pool.Begin(ctx)
	if err != nil {
		writeFailure(w, r, dbFailure(ctx, err), "")
		return
	}
	defer tx.Rollback(ctx)
	q := query.New(tx)
	b, err := q.LockBatch(ctx, query.LockBatchParams{ID: id, UserID: user})
	if err != nil {
		writeFailure(w, r, dbFailure(ctx, err), "")
		return
	}

	var fail *failure
	switch r.Method {
	case http.MethodPost:
		b, fail = h.svc.commit(ctx, tx, b, input.Revision)
	case http.MethodPatch:
		b, fail = h.svc.edit(ctx, tx, b, input)
	case http.MethodDelete:
		fail = cancelBatch(ctx, q, b)
	}
	if fail != nil {
		if r.Method == http.MethodPost {
			statementAttempts.WithLabelValues("commit", fail.code).Inc()
		}
		writeFailure(w, r, fail, id.String())
		return
	}
	if err := tx.Commit(ctx); err != nil {
		writeFailure(w, r, dbFailure(ctx, err), id.String())
		return
	}
	switch r.Method {
	case http.MethodPost:
		statementAttempts.WithLabelValues("commit", "committed").Inc()
		webutil.WriteJSON(w, http.StatusOK, committed(b))
	case http.MethodPatch:
		webutil.WriteJSON(w, http.StatusOK, summary(b))
	case http.MethodDelete:
		w.WriteHeader(http.StatusNoContent)
	}
}

func readEdit(w http.ResponseWriter, r *http.Request, input *editInput) bool {
	d := json.NewDecoder(http.MaxBytesReader(w, r.Body, 2<<20))
	d.DisallowUnknownFields()
	return d.Decode(input) == nil && input.Revision > 0 && len(input.Changes) <= 10_000 && errors.Is(d.Decode(new(any)), io.EOF)
}

func cancelBatch(ctx context.Context, q *query.Queries, b query.StatementImportBatch) *failure {
	if b.Status == "committed" {
		return notReady
	}
	if err := q.CancelBatch(ctx, b.ID); err != nil {
		return dbFailure(ctx, err)
	}
	if err := q.RemoveRows(ctx, b.ID); err != nil {
		return dbFailure(ctx, err)
	}
	if err := q.RemoveStructure(ctx, b.ID); err != nil && !errors.Is(err, pgx.ErrNoRows) {
		return dbFailure(ctx, err)
	}
	return nil
}
