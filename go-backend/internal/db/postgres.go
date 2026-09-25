package db

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"net"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/stdlib"
)

// OpenSQL serves the two legacy slices while their repositories move to pgxpool.
func OpenSQL(ctx context.Context, dsn string) (*sql.DB, error) {
	config, err := pgx.ParseConfig(dsn)
	if err != nil {
		return nil, err
	}
	config.ConnectTimeout = 5 * time.Second
	config.DialFunc = (&net.Dialer{Timeout: 5 * time.Second, KeepAlive: 30 * time.Second}).DialContext
	db := stdlib.OpenDB(*config, stdlib.OptionResetSession(func(ctx context.Context, conn *pgx.Conn) error {
		pingCtx, cancel := context.WithTimeout(ctx, 2*time.Second)
		defer cancel()
		if err := conn.Ping(pingCtx); err != nil {
			return driver.ErrBadConn
		}
		return nil
	}))
	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(5)
	db.SetConnMaxIdleTime(time.Minute)
	db.SetConnMaxLifetime(5 * time.Minute)
	pingCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
	defer cancel()
	if err := db.PingContext(pingCtx); err != nil {
		db.Close()
		return nil, err
	}
	return db, nil
}
