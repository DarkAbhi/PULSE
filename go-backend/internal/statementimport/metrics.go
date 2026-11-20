package statementimport

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

var statementAttempts = promauto.NewCounterVec(prometheus.CounterOpts{
	Name: "pulse_statement_import_attempts_total",
	Help: "Statement import attempts by operation and safe outcome code.",
}, []string{"operation", "outcome"})
