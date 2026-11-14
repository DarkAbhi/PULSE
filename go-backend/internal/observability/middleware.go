package observability

import (
	"bufio"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/prometheus/client_golang/prometheus"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/codes"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/semconv/v1.26.0"
	"go.opentelemetry.io/otel/trace"
)

// responseWriter wraps http.ResponseWriter to capture status and bytes written.
type responseWriter struct {
	http.ResponseWriter
	status           int
	bytes            int64
	hasWrittenHeader bool
}

func newResponseWriter(w http.ResponseWriter) *responseWriter {
	return &responseWriter{ResponseWriter: w, status: http.StatusOK}
}

func (rw *responseWriter) WriteHeader(code int) {
	if !rw.hasWrittenHeader {
		rw.status = code
		rw.hasWrittenHeader = true
		rw.ResponseWriter.WriteHeader(code)
	}
}

func (rw *responseWriter) Write(b []byte) (int, error) {
	if !rw.hasWrittenHeader {
		rw.WriteHeader(http.StatusOK)
	}
	n, err := rw.ResponseWriter.Write(b)
	rw.bytes += int64(n)
	return n, err
}

func (rw *responseWriter) Flush() {
	if f, ok := rw.ResponseWriter.(http.Flusher); ok {
		f.Flush()
	}
}

func (rw *responseWriter) Hijack() (net.Conn, *bufio.ReadWriter, error) {
	if hj, ok := rw.ResponseWriter.(http.Hijacker); ok {
		return hj.Hijack()
	}
	return nil, nil, fmt.Errorf("hijacker not supported")
}

// HTTPMiddleware provides unified OpenTelemetry tracing, Prometheus metrics,
// and structured slog logging for incoming HTTP requests.
func HTTPMiddleware(serviceName string) func(http.Handler) http.Handler {
	tracer := Tracer()
	propagator := otel.GetTextMapPropagator()

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()

			// Extract trace context from request headers (W3C traceparent)
			ctx := propagator.Extract(r.Context(), propagation.HeaderCarrier(r.Header))

			// Initial span name using method; will update after route matching
			spanName := fmt.Sprintf("HTTP %s", r.Method)
			ctx, span := tracer.Start(ctx, spanName,
				trace.WithSpanKind(trace.SpanKindServer),
			)
			defer span.End()

			// Set standard semantic conventions
			span.SetAttributes(
				semconv.HTTPRequestMethodKey.String(r.Method),
				semconv.URLPath(r.URL.Path),
				semconv.ClientAddress(clientIP(r)),
				semconv.UserAgentOriginal(r.UserAgent()),
			)

			// Update request context with span and contextual logger
			reqLogger := slog.Default().With(
				"method", r.Method,
				"path", r.URL.Path,
			)
			ctx = WithLogger(ctx, reqLogger)
			r = r.WithContext(ctx)

			// In-flight metrics gauge
			HTTPRequestsInFlight.WithLabelValues(r.Method).Inc()
			defer HTTPRequestsInFlight.WithLabelValues(r.Method).Dec()

			rw := newResponseWriter(w)

			// Execute downstream handlers
			next.ServeHTTP(rw, r)

			duration := time.Since(start)
			durationSec := duration.Seconds()
			statusStr := strconv.Itoa(rw.status)

			// Extract Chi matched route pattern to prevent high-cardinality label explosion
			routePattern := ""
			if rctx := chi.RouteContext(r.Context()); rctx != nil {
				routePattern = rctx.RoutePattern()
			}
			if routePattern == "" {
				routePattern = normalizePath(r.URL.Path)
			}

			// Update span with resolved route pattern and status code
			span.SetName(fmt.Sprintf("%s %s", r.Method, routePattern))
			span.SetAttributes(
				semconv.HTTPRoute(routePattern),
				semconv.HTTPResponseStatusCode(rw.status),
				attribute.Int64("http.response_size_bytes", rw.bytes),
			)

			if rw.status >= http.StatusInternalServerError {
				span.SetStatus(codes.Error, http.StatusText(rw.status))
			} else {
				span.SetStatus(codes.Ok, "")
			}

			// Record Prometheus metrics with Exemplars linked to Trace ID
			traceID := ""
			if span.SpanContext().IsValid() {
				traceID = span.SpanContext().TraceID().String()
			}

			// Duration Histogram observation with Exemplar if available
			durationObserver := HTTPRequestDuration.WithLabelValues(r.Method, routePattern, statusStr)
			if eo, ok := durationObserver.(prometheus.ExemplarObserver); ok && traceID != "" {
				eo.ObserveWithExemplar(durationSec, prometheus.Labels{"trace_id": traceID})
			} else {
				durationObserver.Observe(durationSec)
			}

			// Requests Total Counter
			HTTPRequestsTotal.WithLabelValues(r.Method, routePattern, statusStr).Inc()

			// Response Size Histogram
			HTTPResponseSizeBytes.WithLabelValues(r.Method, routePattern).Observe(float64(rw.bytes))

			// Log HTTP completion with structured slog
			logFields := []any{
				"status", rw.status,
				"duration_ms", duration.Milliseconds(),
				"bytes", rw.bytes,
				"route", routePattern,
				"ip", clientIP(r),
			}

			// Health and metrics scrapes log at debug to avoid polluting access logs
			if isProbeOrMetricPath(r.URL.Path) {
				slog.DebugContext(ctx, "http_request", logFields...)
			} else if rw.status >= http.StatusInternalServerError {
				slog.ErrorContext(ctx, "http_request_error", logFields...)
			} else {
				slog.InfoContext(ctx, "http_request", logFields...)
			}
		})
	}
}

// clientIP extracts IP from X-Forwarded-For or RemoteAddr.
func clientIP(r *http.Request) string {
	if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
		parts := strings.Split(xff, ",")
		return strings.TrimSpace(parts[0])
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}

func isProbeOrMetricPath(path string) bool {
	return path == "/healthz" || path == "/readyz" || path == "/metrics"
}

// normalizePath ensures unmatched routes do not cause unbounded Prometheus label cardinality.
func normalizePath(path string) string {
	if path == "" || path == "/" {
		return "/"
	}
	if strings.HasPrefix(path, "/swagger") {
		return "/swagger/*"
	}
	if strings.HasPrefix(path, "/debug/pprof") {
		return "/debug/pprof/*"
	}
	return "unmatched"
}
