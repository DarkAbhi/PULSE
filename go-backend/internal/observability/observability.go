// Package observability configures logs, metrics, tracing, and profiling.
package observability

import (
	"context"
	"fmt"
	"log/slog"
)

// System encapsulates all observability subsystems.
type System struct {
	Config   Config
	Logger   *slog.Logger
	Shutdown func(context.Context) error
}

// Setup initializes the complete observability suite:
// - Parses environment configuration
// - Configures structured slog logger
// - Sets up OpenTelemetry tracer provider & propagators
func Setup(ctx context.Context) (*System, error) {
	cfg := LoadConfigFromEnv()

	// Initialize structured logger first
	logger := InitLogger(cfg)

	// Initialize OpenTelemetry
	shutdownTracer, err := InitTracer(ctx, cfg)
	if err != nil {
		return nil, fmt.Errorf("init tracer: %w", err)
	}

	shutdown := func(shutdownCtx context.Context) error {
		if shutdownTracer != nil {
			if err := shutdownTracer(shutdownCtx); err != nil {
				return fmt.Errorf("shutdown tracer: %w", err)
			}
		}
		return nil
	}

	return &System{
		Config:   cfg,
		Logger:   logger,
		Shutdown: shutdown,
	}, nil
}
