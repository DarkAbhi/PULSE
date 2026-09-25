// Package profile manages user profile names and password changes.
package profile

type profileBody struct {
	Name string `json:"name"`
}
type changePasswordBody struct {
	CurrentPassword string `json:"current_password"`
	NewPassword     string `json:"new_password"`
}
