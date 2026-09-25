// Package activity records daily meditation and sport activity.
package activity

import "errors"

var ErrAlreadyMeditated = errors.New("activity: already meditated today")
var ErrInvalidSport = errors.New("activity: invalid sport")
