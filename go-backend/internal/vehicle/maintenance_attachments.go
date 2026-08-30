package vehicle

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"io"
	"mime"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"

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

func newAttachmentStorage(ctx context.Context) (*attachmentStorage, error) {
	bucket := strings.TrimSpace(os.Getenv("S3_BUCKET"))
	if bucket == "" {
		return nil, nil
	}
	region := strings.TrimSpace(os.Getenv("AWS_REGION"))
	if region == "" {
		return nil, errors.New("AWS_REGION must be set when S3_BUCKET is configured")
	}
	config, err := awsconfig.LoadDefaultConfig(ctx, awsconfig.WithRegion(region))
	if err != nil {
		return nil, err
	}
	options := []func(*s3.Options){}
	if endpoint := strings.TrimSpace(os.Getenv("S3_ENDPOINT")); endpoint != "" {
		options = append(options, func(o *s3.Options) { o.BaseEndpoint = aws.String(endpoint) })
	}
	if os.Getenv("S3_FORCE_PATH_STYLE") == "true" {
		options = append(options, func(o *s3.Options) { o.UsePathStyle = true })
	}
	return &attachmentStorage{bucket: bucket, client: s3.NewFromConfig(config, options...)}, nil
}

func (h *Handler) attachmentStore(ctx context.Context) (*attachmentStorage, error) {
	if h.attachments != nil {
		return h.attachments, nil
	}
	store, err := newAttachmentStorage(ctx)
	if err != nil {
		return nil, err
	}
	if store == nil {
		return nil, errors.New("S3 uploads are not configured")
	}
	h.attachments = store
	return store, nil
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
	store, err := h.attachmentStore(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	key := fmt.Sprintf("garage/user-%d/vehicle-%d/maintenance-%d/%s-%s", user.ID, vehicleID, recordID, uuid.NewString(), fileName)
	_, err = store.client.PutObject(r.Context(), &s3.PutObjectInput{Bucket: aws.String(store.bucket), Key: aws.String(key), Body: io.LimitReader(file, maxMaintenanceAttachmentBytes+1), ContentType: aws.String(contentType), ContentDisposition: aws.String("attachment; filename=\"" + fileName + "\"")})
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	var attachment maintenanceAttachment
	err = h.DB.QueryRow(`INSERT INTO vehicle_maintenance_attachments (maintenance_record_id,user_id,storage_key,file_name,content_type,size_bytes) VALUES ($1,$2,$3,$4,$5,$6) RETURNING id,file_name,content_type,size_bytes,created_at`, recordID, user.ID, key, fileName, contentType, header.Size).Scan(&attachment.ID, &attachment.FileName, &attachment.ContentType, &attachment.SizeBytes, &attachment.CreatedAt)
	if err != nil {
		_, _ = store.client.DeleteObject(r.Context(), &s3.DeleteObjectInput{Bucket: aws.String(store.bucket), Key: aws.String(key)})
		webutil.ServerError(w, err)
		return
	}
	attachment.CreatedAt = attachment.CreatedAt.UTC()
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
	var key string
	err = h.DB.QueryRow(`SELECT a.storage_key FROM vehicle_maintenance_attachments a JOIN vehicle_maintenance_records r ON r.id=a.maintenance_record_id WHERE a.id=$1 AND a.maintenance_record_id=$2 AND r.vehicle_id=$3 AND a.user_id=$4`, attachmentID, recordID, vehicleID, user.ID).Scan(&key)
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	store, err := h.attachmentStore(r.Context())
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
	var key string
	err = h.DB.QueryRow(`SELECT a.storage_key FROM vehicle_maintenance_attachments a JOIN vehicle_maintenance_records r ON r.id=a.maintenance_record_id WHERE a.id=$1 AND a.maintenance_record_id=$2 AND r.vehicle_id=$3 AND a.user_id=$4`, attachmentID, recordID, vehicleID, user.ID).Scan(&key)
	if errors.Is(err, sql.ErrNoRows) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	store, err := h.attachmentStore(r.Context())
	if err != nil {
		webutil.ServerError(w, err)
		return
	}
	if _, err := store.client.DeleteObject(r.Context(), &s3.DeleteObjectInput{Bucket: aws.String(store.bucket), Key: aws.String(key)}); err != nil {
		webutil.ServerError(w, err)
		return
	}
	if _, err := h.DB.Exec(`DELETE FROM vehicle_maintenance_attachments WHERE id=$1 AND user_id=$2`, attachmentID, user.ID); err != nil {
		webutil.ServerError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) deleteMaintenanceAttachmentObjects(ctx context.Context, recordID, userID int64) error {
	rows, err := h.DB.QueryContext(ctx, `SELECT storage_key FROM vehicle_maintenance_attachments WHERE maintenance_record_id=$1 AND user_id=$2`, recordID, userID)
	if err != nil {
		return err
	}
	defer rows.Close()
	keys := []string{}
	for rows.Next() {
		var key string
		if err := rows.Scan(&key); err != nil {
			return err
		}
		keys = append(keys, key)
	}
	if err := rows.Err(); err != nil {
		return err
	}
	if len(keys) == 0 {
		return nil
	}
	store, err := h.attachmentStore(ctx)
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
	var found bool
	if err := h.DB.QueryRowContext(ctx, `SELECT EXISTS(SELECT 1 FROM vehicle_maintenance_records WHERE id=$1 AND vehicle_id=$2 AND user_id=$3)`, recordID, vehicleID, userID).Scan(&found); err != nil {
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
