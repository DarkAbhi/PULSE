// Package purchase manages purchases planned for the next month.
package purchase

import (
	"errors"
	"strings"
)

var ErrInvalidItem = errors.New("purchase: invalid item")

type Item struct {
	ID    int64
	Name  string
	Price float64
	URL   *string
}

func NewItem(name string, price float64, url *string) (Item, error) {
	name = strings.TrimSpace(name)
	if name == "" || len([]rune(name)) > 160 || price < 0 {
		return Item{}, ErrInvalidItem
	}
	return Item{Name: name, Price: price, URL: url}, nil
}
