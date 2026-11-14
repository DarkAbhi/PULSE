package observability

import (
	"log/slog"
	"os"
	"strings"
)

// Config holds observability configuration settings.
type Config struct {
	ServiceName    string
	ServiceVersion string
	Environment    string
	LogLevel       slog.Level
	LogFormat      string // "json" or "text"
	OTLPEndpoint   string // e.g. "localhost:4317"
	IsOTLPInsecure bool
	IsPprofEnabled bool
	PprofAuthUser  string
	PprofAuthPass  string
}

// LoadConfigFromEnv builds Config from environment variables.
func LoadConfigFromEnv() Config {
	env := os.Getenv("APP_ENV")
	if env == "" {
		env = "development"
	}

	serviceName := os.Getenv("OTEL_SERVICE_NAME")
	if serviceName == "" {
		serviceName = os.Getenv("SERVICE_NAME")
	}
	if serviceName == "" {
		serviceName = "life-backend"
	}

	serviceVersion := os.Getenv("OTEL_SERVICE_VERSION")
	if serviceVersion == "" {
		serviceVersion = os.Getenv("SERVICE_VERSION")
	}
	if serviceVersion == "" {
		serviceVersion = "1.0.0"
	}

	logFormat := os.Getenv("LOG_FORMAT")
	if logFormat == "" {
		if env == "production" {
			logFormat = "json"
		} else {
			logFormat = "text"
		}
	}

	levelStr := strings.ToLower(os.Getenv("LOG_LEVEL"))
	var logLevel slog.Level
	switch levelStr {
	case "debug":
		logLevel = slog.LevelDebug
	case "warn", "warning":
		logLevel = slog.LevelWarn
	case "error":
		logLevel = slog.LevelError
	case "info":
		logLevel = slog.LevelInfo
	default:
		if env == "production" {
			logLevel = slog.LevelInfo
		} else {
			logLevel = slog.LevelDebug
		}
	}

	isPprofEnabled := strings.EqualFold(os.Getenv("PPROF_ENABLED"), "true")
	isOTLPInsecure := !strings.EqualFold(os.Getenv("OTEL_EXPORTER_OTLP_INSECURE"), "false")

	return Config{
		ServiceName:    serviceName,
		ServiceVersion: serviceVersion,
		Environment:    env,
		LogLevel:       logLevel,
		LogFormat:      strings.ToLower(logFormat),
		OTLPEndpoint:   os.Getenv("OTEL_EXPORTER_OTLP_ENDPOINT"),
		IsOTLPInsecure: isOTLPInsecure,
		IsPprofEnabled: isPprofEnabled,
		PprofAuthUser:  os.Getenv("PPROF_AUTH_USER"),
		PprofAuthPass:  os.Getenv("PPROF_AUTH_PASS"),
	}
}
