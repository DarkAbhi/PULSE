package db

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"fmt"
	"log"
	"net"
	"os"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/stdlib"
)

var DB *sql.DB

func ConnectDB() *sql.DB {
	dbHost := os.Getenv("DB_HOSTNAME")
	dbPort := os.Getenv("DB_PORT")
	if dbPort == "" {
		dbPort = "5432"
	}
	dbUser := os.Getenv("DB_USERNAME")
	dbPassword := os.Getenv("DB_PASSWORD")
	dbName := os.Getenv("DB_NAME")
	dbSSLMode := os.Getenv("DB_SSLMODE")
	if dbSSLMode == "" {
		dbSSLMode = "disable"
	}

	dsn := fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=%s&connect_timeout=5&timezone=UTC",
		dbUser, dbPassword, dbHost, dbPort, dbName, dbSSLMode,
	)

	connConfig, err := pgx.ParseConfig(dsn)
	if err != nil {
		log.Fatalf("❌ Failed to parse DB connection string: %v", err)
	}

	// Fail fast on network connects (5s instead of OS default 75s) and send TCP keep-alives
	connConfig.ConnectTimeout = 5 * time.Second
	connConfig.DialFunc = (&net.Dialer{
		Timeout:   5 * time.Second,
		KeepAlive: 30 * time.Second,
	}).DialContext

	// Verify connections when retrieved from the pool; discard and reconnect if severed
	db := stdlib.OpenDB(*connConfig, stdlib.OptionResetSession(func(ctx context.Context, conn *pgx.Conn) error {
		pingCtx, cancel := context.WithTimeout(ctx, 2*time.Second)
		defer cancel()
		if err := conn.Ping(pingCtx); err != nil {
			return driver.ErrBadConn
		}
		return nil
	}))

	// Connection pool settings
	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(5)
	db.SetConnMaxIdleTime(1 * time.Minute)
	db.SetConnMaxLifetime(5 * time.Minute)

	// Verify connection
	pingCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := db.PingContext(pingCtx); err != nil {
		log.Fatalf("❌ Failed to ping DB: %v", err)
	}

	log.Println("✅ Connected to PostgreSQL database")
	DB = db
	return DB
}
