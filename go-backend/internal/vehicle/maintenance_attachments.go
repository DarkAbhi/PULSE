package vehicle

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"mime"
	"net/http"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"

	"github.com/DarkAbhi/life-backend/internal/vehicle/query"
	"github.com/DarkAbhi/life-backend/internal/webutil"
)

const maxMaintenanceAttachmentBytes = 10 << 20

type maintenanceAttachment struct {
	ID          int64     `json:"id"`
	FileName    string    `json:"file_name"`
	ContentType string    `json:"content_type"`
	SizeBytes   int64     `json:"size_bytes"`
	CreatedAt   time.Time `json:"created_at"`
}

type attachmentStorage struct {
	bucket string
	client *s3.Client
}

type AttachmentConfig struct {
	Bucket         string
	Region         string
	Endpoint       string
	ForcePathStyle bool
}

func newAttachmentStorage(ctx context.Context, settings AttachmentConfig) (*attachmentStorage, error) {
	bucket := strings.TrimSpace(settings.Bucket)
	if bucket == "" {
		return nil, nil
	}
	region := strings.TrimSpace(settings.Region)
	if region == "" {
		return nil, errors.New("AWS_REGION must be set when S3_BUCKET is configured")
	}
	config, err := awsconfig.LoadDefaultConfig(ctx, awsconfig.WithRegion(region))
	if err != nil {
		return nil, err
	}
	options := []func(*s3.Options){}
	if endpoint := strings.TrimSpace(settings.Endpoint); endpoint != "" {
		options = append(options, func(o *s3.Options) { o.BaseEndpoint = aws.String(endpoint) })
	}
	if settings.ForcePathStyle {
		options = append(options, func(o *s3.Options) { o.UsePathStyle = true })
	}
	return &attachmentStorage{bucket: bucket, client: s3.NewFromConfig(config, options...)}, nil
}

func (h *Handler) ConfigureAttachments(ctx context.Context, settings AttachmentConfig) error {
	store, err := newAttachmentStorage(ctx, settings)
	if err != nil {
		return err
	}
	h.attachments = store
	return nil
}

func (h *Handler) attachmentStore() (*attachmentStorage, error) {
	if h.attachments == nil {
		return nil, errors.New("S3 uploads are not configured")
	}
	return h.attachments, nil
}

func (h *Handler) CreateMaintenanceAttachment(w http.ResponseWriter, r *http.Request) {
	user, ok := h.maintenanceUser(w, r)
	if !ok {
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	recordID, err := parseMaintenanceRouteID(r, "recordID")
	if err != nil {
		webutil.BadRequest(w, "invalid maintenance record id")
		return
	}
	if !h.ownsMaintenanceRecord(r.Context(), recordID, vehicleID, user.ID) {
		http.NotFound(w, r)
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, maxMaintenanceAttachmentBytes+(1<<20))
	if err := r.ParseMultipartForm(maxMaintenanceAttachmentBytes); err != nil {
		webutil.BadRequest(w, "attachment must be 10 MB or smaller")
		return
	}
	file, header, err := r.FormFile("file")
	if err != nil {
		webutil.BadRequest(w, "a file is required")
		return
	}
	defer file.Close()
	if header.Size <= 0 || header.Size > maxMaintenanceAttachmentBytes {
		webutil.BadRequest(w, "attachment must be between 1 byte and 10 MB")
		return
	}
	fileName := filepath.Base(strings.TrimSpace(header.Filename))
	if fileName == "." || fileName == "" || len(fileName) > 255 {
		webutil.BadRequest(w, "invalid file name")
		return
	}
	contentType := header.Header.Get("Content-Type")
	if contentType == "" {
		contentType = mime.TypeByExtension(filepath.Ext(fileName))
	}
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	store, err := h.attachmentStore()
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	key := fmt.Sprintf("garage/user-%d/vehicle-%d/maintenance-%d/%s-%s", user.ID, vehicleID, recordID, uuid.NewString(), fileName)
	_, err = store.client.PutObject(r.Context(), &s3.PutObjectInput{
		Bucket:             aws.String(store.bucket),
		Key:                aws.String(key),
		Body:               file,
		ContentLength:      aws.Int64(header.Size),
		ContentType:        aws.String(contentType),
		ContentDisposition: aws.String("attachment; filename=\"" + fileName + "\""),
	})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	row, err := query.New(h.DB).CreateMaintenanceAttachment(r.Context(), query.CreateMaintenanceAttachmentParams{MaintenanceRecordID: recordID, UserID: user.ID, StorageKey: key, FileName: fileName, ContentType: contentType, SizeBytes: header.Size})
	if err != nil {
		_, _ = store.client.DeleteObject(r.Context(), &s3.DeleteObjectInput{Bucket: aws.String(store.bucket), Key: aws.String(key)})
		webutil.ServerError(w, err)
		return
	}
	attachment := maintenanceAttachment{ID: row.ID, FileName: row.FileName, ContentType: row.ContentType, SizeBytes: row.SizeBytes, CreatedAt: row.CreatedAt.Time.UTC()}
	webutil.WriteJSON(w, http.StatusCreated, attachment)
}

func (h *Handler) DownloadMaintenanceAttachment(w http.ResponseWriter, r *http.Request) {
	user, ok := h.maintenanceUser(w, r)
	if !ok {
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	recordID, err := parseMaintenanceRouteID(r, "recordID")
	if err != nil {
		webutil.BadRequest(w, "invalid maintenance record id")
		return
	}
	attachmentID, err := parseMaintenanceRouteID(r, "attachmentID")
	if err != nil {
		webutil.BadRequest(w, "invalid attachment id")
		return
	}
	key, err := query.New(h.DB).GetMaintenanceAttachmentKey(r.Context(), query.GetMaintenanceAttachmentKeyParams{ID: attachmentID, MaintenanceRecordID: recordID, VehicleID: vehicleID, UserID: user.ID})
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	store, err := h.attachmentStore()
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	presigner := s3.NewPresignClient(store.client)
	request, err := presigner.PresignGetObject(r.Context(), &s3.GetObjectInput{Bucket: aws.String(store.bucket), Key: aws.String(key)}, s3.WithPresignExpires(5*time.Minute))
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	http.Redirect(w, r, request.URL, http.StatusFound)
}

func (h *Handler) DeleteMaintenanceAttachment(w http.ResponseWriter, r *http.Request) {
	user, ok := h.maintenanceUser(w, r)
	if !ok {
		return
	}
	vehicleID, ok := webutil.ParseID(w, r)
	if !ok {
		return
	}
	recordID, err := parseMaintenanceRouteID(r, "recordID")
	if err != nil {
		webutil.BadRequest(w, "invalid maintenance record id")
		return
	}
	attachmentID, err := parseMaintenanceRouteID(r, "attachmentID")
	if err != nil {
		webutil.BadRequest(w, "invalid attachment id")
		return
	}
	key, err := query.New(h.DB).GetMaintenanceAttachmentKey(r.Context(), query.GetMaintenanceAttachmentKeyParams{ID: attachmentID, MaintenanceRecordID: recordID, VehicleID: vehicleID, UserID: user.ID})
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	store, err := h.attachmentStore()
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if _, err := store.client.DeleteObject(r.Context(), &s3.DeleteObjectInput{Bucket: aws.String(store.bucket), Key: aws.String(key)}); err != nil {
		webutil.ServerError(w, err)
		return
	}
	if err := query.New(h.DB).DeleteMaintenanceAttachment(r.Context(), query.DeleteMaintenanceAttachmentParams{ID: attachmentID, UserID: user.ID}); err != nil {
		webutil.ServerError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) deleteMaintenanceAttachmentObjects(ctx context.Context, recordID, userID int64) error {
	rows, err := query.New(h.DB).ListMaintenanceAttachmentKeys(ctx, query.ListMaintenanceAttachmentKeysParams{MaintenanceRecordID: recordID, UserID: userID})
	if err != nil {
		return err
	}
	keys := rows
	if len(keys) == 0 {
		return nil
	}
	store, err := h.attachmentStore()
	if err != nil {
		return err
	}
	for _, key := range keys {
		if _, err := store.client.DeleteObject(ctx, &s3.DeleteObjectInput{Bucket: aws.String(store.bucket), Key: aws.String(key)}); err != nil {
			return err
		}
	}
	return nil
}

func (h *Handler) ownsMaintenanceRecord(ctx context.Context, recordID, vehicleID, userID int64) bool {
	found, err := query.New(h.DB).OwnsMaintenanceRecord(ctx, query.OwnsMaintenanceRecordParams{ID: recordID, VehicleID: vehicleID, UserID: userID})
	if err != nil {
		return false
	}
	return found
}

func parseMaintenanceRouteID(r *http.Request, key string) (int64, error) {
	value, err := strconv.ParseInt(chi.URLParam(r, key), 10, 64)
	if err != nil || value <= 0 {
		return 0, errors.New("invalid id")
	}
	return value, nil
}
