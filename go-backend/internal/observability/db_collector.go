package observability

import (
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/prometheus/client_golang/prometheus"
)

// DBStatsCollector collects metrics from a pgx connection pool.
type DBStatsCollector struct {
	pool *pgxpool.Pool

	totalConns          *prometheus.Desc
	idleConns           *prometheus.Desc
	acquiredConns       *prometheus.Desc
	maxConns            *prometheus.Desc
	acquireCount        *prometheus.Desc
	emptyAcquireCount   *prometheus.Desc
	acquireDurationNanos *prometheus.Desc
}

// NewDBStatsCollector creates a new DBStatsCollector for pgxpool.
func NewDBStatsCollector(pool *pgxpool.Pool) *DBStatsCollector {
	return &DBStatsCollector{
		pool: pool,
		totalConns: prometheus.NewDesc(
			"life_db_pool_total_connections",
			"Current number of established connections in the PostgreSQL pool.",
			nil, nil,
		),
		idleConns: prometheus.NewDesc(
			"life_db_pool_idle_connections",
			"Current number of idle connections in the PostgreSQL pool.",
			nil, nil,
		),
		acquiredConns: prometheus.NewDesc(
			"life_db_pool_acquired_connections",
			"Current number of acquired connections currently in use by queries.",
			nil, nil,
		),
		maxConns: prometheus.NewDesc(
			"life_db_pool_max_connections",
			"Configured maximum number of connections for the pool.",
			nil, nil,
		),
		acquireCount: prometheus.NewDesc(
			"life_db_pool_acquire_total",
			"Cumulative number of successful connection acquisitions from the pool.",
			nil, nil,
		),
		emptyAcquireCount: prometheus.NewDesc(
			"life_db_pool_empty_acquire_total",
			"Cumulative number of times an acquisition had to wait for a connection.",
			nil, nil,
		),
		acquireDurationNanos: prometheus.NewDesc(
			"life_db_pool_acquire_duration_seconds_total",
			"Total duration in seconds spent waiting for connection acquisitions.",
			nil, nil,
		),
	}
}

// Describe sends the super-set of all possible descriptors of metrics collected by this Collector.
func (c *DBStatsCollector) Describe(ch chan<- *prometheus.Desc) {
	ch <- c.totalConns
	ch <- c.idleConns
	ch <- c.acquiredConns
	ch <- c.maxConns
	ch <- c.acquireCount
	ch <- c.emptyAcquireCount
	ch <- c.acquireDurationNanos
}

// Collect is called by the Prometheus registry when collecting metrics.
func (c *DBStatsCollector) Collect(ch chan<- prometheus.Metric) {
	if c.pool == nil {
		return
	}

	stat := c.pool.Stat()

	ch <- prometheus.MustNewConstMetric(c.totalConns, prometheus.GaugeValue, float64(stat.TotalConns()))
	ch <- prometheus.MustNewConstMetric(c.idleConns, prometheus.GaugeValue, float64(stat.IdleConns()))
	ch <- prometheus.MustNewConstMetric(c.acquiredConns, prometheus.GaugeValue, float64(stat.AcquiredConns()))
	ch <- prometheus.MustNewConstMetric(c.maxConns, prometheus.GaugeValue, float64(stat.MaxConns()))
	ch <- prometheus.MustNewConstMetric(c.acquireCount, prometheus.CounterValue, float64(stat.AcquireCount()))
	ch <- prometheus.MustNewConstMetric(c.emptyAcquireCount, prometheus.CounterValue, float64(stat.EmptyAcquireCount()))
	ch <- prometheus.MustNewConstMetric(c.acquireDurationNanos, prometheus.CounterValue, stat.AcquireDuration().Seconds())
}

// RegisterDBStatsCollector registers the pgxpool collector with the default Prometheus registry.
func RegisterDBStatsCollector(pool *pgxpool.Pool) {
	if pool != nil {
		prometheus.MustRegister(NewDBStatsCollector(pool))
	}
}
