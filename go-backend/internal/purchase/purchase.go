package purchase

import (
	"errors"
	"strings"
)

var ErrInvalidInput = errors.New("invalid purchase")

type Purchase struct {
	ID    int64
	Name  string
	Price float64
	URL   *string
}

func NewPurchase(name string, price float64, url *string) (Purchase, error) {
	name = strings.TrimSpace(name)
	if name == "" || len([]rune(name)) > 160 || price < 0 {
		return Purchase{}, ErrInvalidInput
	}
	return Purchase{Name: name, Price: price, URL: url}, nil
}
