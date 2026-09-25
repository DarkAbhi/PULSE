package purchase

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/DarkAbhi/life-backend/internal/purchase/query"
)

type Repository struct{ queries *query.Queries }

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{queries: query.New(db)}
}

func (r *Repository) List(ctx context.Context, userID int64, month string) ([]Item, error) {
	rows, err := r.queries.ListPurchases(ctx, query.ListPurchasesParams{UserID: userID, Column2: month})
	if err != nil {
		return nil, err
	}
	items := make([]Item, 0, len(rows))
	for _, row := range rows {
		items = append(items, Item{ID: row.ID, Name: row.Name, Price: row.Price, URL: row.Url})
	}
	return items, nil
}

func (r *Repository) Create(ctx context.Context, userID int64, month string, item Item) (Item, error) {
	row, err := r.queries.CreatePurchase(ctx, query.CreatePurchaseParams{
		UserID: userID, Column2: month, Name: item.Name, Price: item.Price, Url: item.URL,
	})
	return Item{ID: row.ID, Name: row.Name, Price: row.Price, URL: row.Url}, err
}

func (r *Repository) Delete(ctx context.Context, userID, id int64, month string) (bool, error) {
	count, err := r.queries.DeletePurchase(ctx, query.DeletePurchaseParams{ID: id, UserID: userID, Column3: month})
	return count > 0, err
}

func (r *Repository) Clear(ctx context.Context, userID int64, month string) error {
	return r.queries.ClearPurchases(ctx, query.ClearPurchasesParams{UserID: userID, Column2: month})
}
