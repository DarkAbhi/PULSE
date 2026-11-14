package observability

import (
	"context"
	"log/slog"
	"os"

	"go.opentelemetry.io/otel/trace"
)

type contextKey struct{}

var loggerContextKey = contextKey{}

// TraceHandler wraps an slog.Handler to inject OpenTelemetry trace_id and span_id
// into log records when a valid span exists in the context.
type TraceHandler struct {
	slog.Handler
}

// NewTraceHandler wraps the given handler with trace correlation.
func NewTraceHandler(h slog.Handler) *TraceHandler {
	return &TraceHandler{Handler: h}
}

// Handle adds trace_id and span_id attributes if present in the context.
func (th *TraceHandler) Handle(ctx context.Context, r slog.Record) error {
	if ctx != nil {
		span := trace.SpanFromContext(ctx)
		if span.SpanContext().IsValid() {
			r.AddAttrs(
				slog.String("trace_id", span.SpanContext().TraceID().String()),
				slog.String("span_id", span.SpanContext().SpanID().String()),
			)
		}
	}
	return th.Handler.Handle(ctx, r)
}

// WithAttrs returns a new handler with the given attributes.
func (th *TraceHandler) WithAttrs(attrs []slog.Attr) slog.Handler {
	return &TraceHandler{Handler: th.Handler.WithAttrs(attrs)}
}

// WithGroup returns a new handler with the given group name.
func (th *TraceHandler) WithGroup(name string) slog.Handler {
	return &TraceHandler{Handler: th.Handler.WithGroup(name)}
}

// InitLogger configures and sets the global default slog.Logger.
func InitLogger(cfg Config) *slog.Logger {
	opts := &slog.HandlerOptions{
		Level: cfg.LogLevel,
	}

	var baseHandler slog.Handler
	if cfg.LogFormat == "json" {
		baseHandler = slog.NewJSONHandler(os.Stdout, opts)
	} else {
		baseHandler = slog.NewTextHandler(os.Stdout, opts)
	}

	traceHandler := NewTraceHandler(baseHandler)
	logger := slog.New(traceHandler)
	slog.SetDefault(logger)
	return logger
}

// WithLogger returns a child context containing the logger.
func WithLogger(ctx context.Context, logger *slog.Logger) context.Context {
	return context.WithValue(ctx, loggerContextKey, logger)
}

// FromContext retrieves the logger from context, or returns slog.Default().
func FromContext(ctx context.Context) *slog.Logger {
	if ctx != nil {
		if l, ok := ctx.Value(loggerContextKey).(*slog.Logger); ok && l != nil {
			return l
		}
	}
	return slog.Default()
}
