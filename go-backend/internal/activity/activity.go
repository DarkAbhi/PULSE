// Package activity records daily meditation and sport activity.
package activity

import "errors"

var ErrAlreadyMeditated = errors.New("already meditated today")
var ErrInvalidSport = errors.New("invalid sport")
