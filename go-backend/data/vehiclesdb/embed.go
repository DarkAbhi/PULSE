// Package vehiclesdb holds the pinned, offline reference-data seed.
package vehiclesdb

import _ "embed"

// Seed only inserts missing catalog records; it never changes personal vehicles.
//
//go:embed seed.sql
var Seed string
