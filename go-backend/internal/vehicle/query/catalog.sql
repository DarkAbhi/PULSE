-- name: GetCatalogModel :one
SELECT * FROM vehicle_catalog WHERE id=$1;
