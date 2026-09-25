package observability

import (
	"context"
	"fmt"
	"log/slog"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.26.0"
	"go.opentelemetry.io/otel/trace"
	"google.golang.org/grpc/credentials/insecure"
)

const defaultTracerName = "github.com/DarkAbhi/life-backend"

// InitTracer initializes OpenTelemetry tracing and sets global propagators.
// If no OTLPEndpoint is specified, a local TracerProvider is configured so spans
// and trace IDs are still generated for log correlation and exemplars.
func InitTracer(ctx context.Context, cfg Config) (func(context.Context) error, error) {
	// Configure W3C standard trace propagation
	otel.SetTextMapPropagator(
		propagation.NewCompositeTextMapPropagator(
			propagation.TraceContext{},
			propagation.Baggage{},
		),
	)

	res, err := resource.New(ctx,
		resource.WithSchemaURL(semconv.SchemaURL),
		resource.WithAttributes(
			semconv.ServiceNameKey.String(cfg.ServiceName),
			semconv.ServiceVersionKey.String(cfg.ServiceVersion),
			semconv.DeploymentEnvironmentKey.String(cfg.Environment),
		),
	)
	if err != nil {
		return nil, fmt.Errorf("create otel resource: %w", err)
	}

	var tpOptions []sdktrace.TracerProviderOption
	tpOptions = append(tpOptions, sdktrace.WithResource(res))

	if cfg.OTLPEndpoint != "" {
		grpcOpts := []otlptracegrpc.Option{
			otlptracegrpc.WithEndpoint(cfg.OTLPEndpoint),
		}
		if cfg.OTLPInsecure {
			grpcOpts = append(grpcOpts, otlptracegrpc.WithTLSCredentials(insecure.NewCredentials()))
		}
		exporter, err := otlptracegrpc.New(ctx, grpcOpts...)
		if err != nil {
			return nil, fmt.Errorf("create otlp trace exporter: %w", err)
		}
		tpOptions = append(tpOptions, sdktrace.WithBatcher(exporter))
		slog.Info("opentelemetry exporter connected", "endpoint", cfg.OTLPEndpoint)
	} else {
		slog.Debug("opentelemetry running in local trace ID mode (no OTLP endpoint configured)")
	}

	tp := sdktrace.NewTracerProvider(tpOptions...)
	otel.SetTracerProvider(tp)

	shutdown := func(shutdownCtx context.Context) error {
		return tp.Shutdown(shutdownCtx)
	}

	return shutdown, nil
}

// Tracer returns the default application Tracer.
func Tracer() trace.Tracer {
	return otel.Tracer(defaultTracerName)
}

// StartSpan creates a new child span using the default tracer.
func StartSpan(ctx context.Context, spanName string, opts ...trace.SpanStartOption) (context.Context, trace.Span) {
	return Tracer().Start(ctx, spanName, opts...)
}
