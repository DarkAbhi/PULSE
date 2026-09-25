package observability

import (
	"bytes"
	"context"
	"encoding/json"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/prometheus/client_golang/prometheus/testutil"
	"go.opentelemetry.io/otel"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	"go.opentelemetry.io/otel/trace"
)

func TestLoadConfigFromEnv(t *testing.T) {
	os.Setenv("APP_ENV", "production")
	os.Setenv("LOG_LEVEL", "warn")
	os.Setenv("LOG_FORMAT", "json")
	os.Setenv("PPROF_ENABLED", "true")
	os.Setenv("PPROF_AUTH_USER", "admin")
	os.Setenv("PPROF_AUTH_PASS", "secret")
	defer func() {
		os.Unsetenv("APP_ENV")
		os.Unsetenv("LOG_LEVEL")
		os.Unsetenv("LOG_FORMAT")
		os.Unsetenv("PPROF_ENABLED")
		os.Unsetenv("PPROF_AUTH_USER")
		os.Unsetenv("PPROF_AUTH_PASS")
	}()

	cfg := LoadConfigFromEnv()
	if cfg.Environment != "production" {
		t.Fatalf("expected production, got %s", cfg.Environment)
	}
	if cfg.LogLevel != slog.LevelWarn {
		t.Fatalf("expected warn level, got %v", cfg.LogLevel)
	}
	if cfg.LogFormat != "json" {
		t.Fatalf("expected json format, got %s", cfg.LogFormat)
	}
	if !cfg.IsPprofEnabled {
		t.Fatal("expected pprof enabled")
	}
	if cfg.PprofAuthUser != "admin" || cfg.PprofAuthPass != "secret" {
		t.Fatal("unexpected pprof auth credentials")
	}
}

func TestTraceHandlerInjectsTraceID(t *testing.T) {
	tp := sdktrace.NewTracerProvider()
	otel.SetTracerProvider(tp)
	tracer := tp.Tracer("test-tracer")

	ctx, span := tracer.Start(context.Background(), "test-span")
	defer span.End()

	var buf bytes.Buffer
	base := slog.NewJSONHandler(&buf, &slog.HandlerOptions{Level: slog.LevelDebug})
	th := NewTraceHandler(base)
	logger := slog.New(th)

	logger.InfoContext(ctx, "hello trace correlation", "key", "val")

	var output map[string]any
	if err := json.Unmarshal(buf.Bytes(), &output); err != nil {
		t.Fatalf("invalid json: %v", err)
	}

	traceID, ok := output["trace_id"].(string)
	if !ok || traceID == "" {
		t.Fatalf("expected trace_id in log, got %v", output)
	}
	spanID, ok := output["span_id"].(string)
	if !ok || spanID == "" {
		t.Fatalf("expected span_id in log, got %v", output)
	}
	if traceID != span.SpanContext().TraceID().String() {
		t.Fatalf("trace_id mismatch: got %s, want %s", traceID, span.SpanContext().TraceID().String())
	}
}

func TestHTTPMiddlewareMetricsAndLogging(t *testing.T) {
	r := chi.NewRouter()
	r.Use(HTTPMiddleware("test-service"))

	r.Get("/api/items/{id}", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"status":"ok"}`))
	})
	r.Get("/api/fail", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte(`{"error":"boom"}`))
	})

	// Test successful route
	req := httptest.NewRequest(http.MethodGet, "/api/items/42", nil)
	rec := httptest.NewRecorder()
	r.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}

	// Verify route pattern is normalized in counter label
	count := testutil.ToFloat64(HTTPRequestsTotal.WithLabelValues("GET", "/api/items/{id}", "200"))
	if count < 1 {
		t.Fatalf("expected HTTPRequestsTotal counter >= 1, got %f", count)
	}

	// Test 500 error route
	reqErr := httptest.NewRequest(http.MethodGet, "/api/fail", nil)
	recErr := httptest.NewRecorder()
	r.ServeHTTP(recErr, reqErr)

	if recErr.Code != http.StatusInternalServerError {
		t.Fatalf("expected 500, got %d", recErr.Code)
	}
	countErr := testutil.ToFloat64(HTTPRequestsTotal.WithLabelValues("GET", "/api/fail", "500"))
	if countErr < 1 {
		t.Fatalf("expected HTTPRequestsTotal error counter >= 1, got %f", countErr)
	}
}

func TestPprofBasicAuth(t *testing.T) {
	cfg := Config{
		IsPprofEnabled: true,
		PprofAuthUser:  "monitor",
		PprofAuthPass:  "secret123",
	}

	r := chi.NewRouter()
	RegisterPprofRoutes(r, cfg)

	// Unauthenticated request should receive 401 Unauthorized
	req := httptest.NewRequest(http.MethodGet, "/debug/pprof/", nil)
	rec := httptest.NewRecorder()
	r.ServeHTTP(rec, req)
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected 401, got %d", rec.Code)
	}

	// Authenticated request should receive 200 OK
	reqAuth := httptest.NewRequest(http.MethodGet, "/debug/pprof/", nil)
	reqAuth.SetBasicAuth("monitor", "secret123")
	recAuth := httptest.NewRecorder()
	r.ServeHTTP(recAuth, reqAuth)
	if recAuth.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", recAuth.Code)
	}
}

func TestMetricsEndpoint(t *testing.T) {
	handler := MetricsHandler()
	req := httptest.NewRequest(http.MethodGet, "/metrics", nil)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200 from /metrics, got %d", rec.Code)
	}
	body := rec.Body.String()
	if !bytes.Contains([]byte(body), []byte("life_http_requests_total")) {
		t.Fatalf("expected life_http_requests_total in /metrics output")
	}
}

func TestTracerAndSpanHelpers(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()

	cfg := Config{
		ServiceName:    "test-service",
		ServiceVersion: "0.1.0",
		Environment:    "test",
	}
	shutdown, err := InitTracer(ctx, cfg)
	if err != nil {
		t.Fatalf("init tracer failed: %v", err)
	}
	defer func() { _ = shutdown(context.Background()) }()

	spanCtx, span := StartSpan(ctx, "test-op")
	if span == nil {
		t.Fatal("expected valid span")
	}
	defer span.End()

	currentSpan := trace.SpanFromContext(spanCtx)
	if !currentSpan.SpanContext().IsValid() {
		t.Fatal("expected span context to be valid in context")
	}
}
