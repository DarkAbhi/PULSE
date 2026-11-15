package statementimport

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"mime/multipart"
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5/middleware"
)

const maxPDFSize = 10 << 20
const maxDocumentSize = 32 << 20

type extractor struct {
	url    string
	client *http.Client
}

func (e extractor) extract(ctx context.Context, pdf []byte) (Document, []byte, *failure) {
	var body bytes.Buffer
	form := multipart.NewWriter(&body)
	file, err := form.CreateFormFile("file", "statement.pdf")
	if err != nil {
		return Document{}, nil, extractFailed
	}
	if _, err := file.Write(pdf); err != nil {
		return Document{}, nil, extractFailed
	}
	if err := form.Close(); err != nil {
		return Document{}, nil, extractFailed
	}
	ctx, cancel := context.WithTimeout(ctx, 150*time.Second)
	defer cancel()
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, strings.TrimRight(e.url, "/")+"/extract", &body)
	if err != nil {
		return Document{}, nil, extractFailed
	}
	req.Header.Set("Content-Type", form.FormDataContentType())
	req.Header.Set("X-Request-Id", middleware.GetReqID(ctx))
	res, err := e.client.Do(req)
	if err != nil {
		if ctx.Err() != nil {
			return Document{}, nil, timedOut
		}
		return Document{}, nil, extractFailed
	}
	defer res.Body.Close()
	if res.StatusCode != 200 {
		var reply struct{ Code string }
		_ = json.NewDecoder(io.LimitReader(res.Body, 4096)).Decode(&reply)
		switch {
		case reply.Code == "ENCRYPTED_PDF_UNSUPPORTED":
			return Document{}, nil, encrypted
		case reply.Code == "INVALID_PDF":
			return Document{}, nil, invalidPDF
		case reply.Code == "OCR_FAILED":
			return Document{}, nil, ocrFailed
		case reply.Code == "PDF_LIMIT_EXCEEDED":
			return Document{}, nil, limitsExceeded
		case res.StatusCode == 429:
			return Document{}, nil, busy
		case res.StatusCode == 504:
			return Document{}, nil, timedOut
		default:
			return Document{}, nil, extractFailed
		}
	}
	data, err := io.ReadAll(io.LimitReader(res.Body, maxDocumentSize+1))
	if err != nil || len(data) > maxDocumentSize {
		if ctx.Err() != nil {
			return Document{}, nil, timedOut
		}
		return Document{}, nil, extractFailed
	}
	var doc Document
	if json.Unmarshal(data, &doc) != nil || doc.PageCount != len(doc.Pages) || doc.PageCount < 1 || doc.PageCount > 100 {
		return Document{}, nil, extractFailed
	}
	count := 0
	for _, page := range doc.Pages {
		count += len(page.Words)
	}
	if count > 100_000 {
		return Document{}, nil, extractFailed
	}
	return doc, data, nil
}
