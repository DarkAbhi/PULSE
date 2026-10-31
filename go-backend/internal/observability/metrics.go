package observability

import (
	"net/http"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// Prometheus Metrics Declarations with PromQL queries documented as comments.

// HTTPRequestsTotal counts total HTTP requests processed.
//
// PromQL (Traffic rate):
//
//	sum(rate(life_http_requests_total[5m]))
//
// PromQL (Error rate percentage):
//
//	sum(rate(life_http_requests_total{status=~"5.."}[5m])) / sum(rate(life_http_requests_total[5m])) * 100
//
// Alert rule:
//
//	sum(rate(life_http_requests_total{status=~"5.."}[5m])) / sum(rate(life_http_requests_total[5m])) > 0.05
var HTTPRequestsTotal = promauto.NewCounterVec(
	prometheus.CounterOpts{
		Namespace: "life",
		Subsystem: "http",
		Name:      "requests_total",
		Help:      "Total number of HTTP requests partitioned by method, path pattern, and status code.",
	},
	[]string{"method", "path", "status"},
)

// HTTPRequestDuration tracks latency of HTTP requests in seconds.
//
// PromQL (P50 / median latency):
//
//	histogram_quantile(0.50, sum(rate(life_http_request_duration_seconds_bucket[5m])) by (le))
//
// PromQL (P90 latency):
//
//	histogram_quantile(0.90, sum(rate(life_http_request_duration_seconds_bucket[5m])) by (le))
//
// PromQL (P99 latency):
//
//	histogram_quantile(0.99, sum(rate(life_http_request_duration_seconds_bucket[5m])) by (le))
//
// PromQL (P99 latency broken down by path):
//
//	histogram_quantile(0.99, sum(rate(life_http_request_duration_seconds_bucket[5m])) by (le, path))
//
// Alert rule (P99 > 1s for 5m):
//
//	histogram_quantile(0.99, sum(rate(life_http_request_duration_seconds_bucket[5m])) by (le)) > 1.0
var HTTPRequestDuration = promauto.NewHistogramVec(
	prometheus.HistogramOpts{
		Namespace: "life",
		Subsystem: "http",
		Name:      "request_duration_seconds",
		Help:      "HTTP request duration distribution in seconds.",
		Buckets: []float64{
			0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0,
		},
	},
	[]string{"method", "path", "status"},
)

// HTTPRequestsInFlight measures the number of HTTP requests currently being handled.
//
// PromQL:
//
//	sum(life_http_requests_in_flight)
var HTTPRequestsInFlight = promauto.NewGaugeVec(
	prometheus.GaugeOpts{
		Namespace: "life",
		Subsystem: "http",
		Name:      "requests_in_flight",
		Help:      "Number of currently in-flight HTTP requests partitioned by method.",
	},
	[]string{"method"},
)

// HTTPResponseSizeBytes tracks the distribution of HTTP response sizes in bytes.
//
// PromQL (Average response size):
//
//	sum(rate(life_http_response_size_bytes_sum[5m])) / sum(rate(life_http_response_size_bytes_count[5m]))
var HTTPResponseSizeBytes = promauto.NewHistogramVec(
	prometheus.HistogramOpts{
		Namespace: "life",
		Subsystem: "http",
		Name:      "response_size_bytes",
		Help:      "Distribution of HTTP response sizes in bytes.",
		Buckets: []float64{
			100, 500, 1000, 5000, 10000, 50000, 100000, 500000, 1000000,
		},
	},
	[]string{"method", "path"},
)

// MetricsHandler returns an http.Handler that serves Prometheus metrics,
// with OpenMetrics support enabled for exemplars.
func MetricsHandler() http.Handler {
	return promhttp.HandlerFor(
		prometheus.DefaultGatherer,
		promhttp.HandlerOpts{
			EnableOpenMetrics: true,
		},
	)
}
