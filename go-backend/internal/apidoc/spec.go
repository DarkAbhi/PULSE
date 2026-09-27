// @Router /api/meal-plans/{id}// @Router /api/meal-plans/{id}/consumed// @Router /api/meal-times/{id}// Package apidoc describes routes registered across the feature packages.
// These types mirror JSON assembled directly by handlers.
package apidoc

import "time"

type Error struct {
	Error string `json:"error"`
}
type Message struct {
	Message string `json:"message"`
}
type Username struct {
	Username string `json:"username"`
}
type LoginInput struct {
	Username string `json:"username"`
	Password string `json:"password"`
}
type Profile struct {
	HasProfile bool    `json:"has_profile"`
	Name       *string `json:"name,omitempty"`
}
type Name struct {
	Name string `json:"name"`
}
type ChangePasswordInput struct {
	CurrentPassword string `json:"current_password"`
	NewPassword     string `json:"new_password"`
}
type SportInput struct {
	Sport string `json:"sport"`
}
type PurchaseInput struct {
	Name  string  `json:"name"`
	Price float64 `json:"price"`
	URL   *string `json:"url"`
}
type PurchaseItem struct {
	ID    int64   `json:"id"`
	Name  string  `json:"name"`
	Price float64 `json:"price"`
	URL   *string `json:"url"`
}
type PurchaseList struct {
	Items []PurchaseItem `json:"items"`
	Total float64        `json:"total"`
}
type Notification struct {
	ID         int64   `json:"id"`
	Source     string  `json:"source"`
	Title      string  `json:"title"`
	Body       *string `json:"body"`
	TargetPath *string `json:"target_path"`
	Priority   int     `json:"priority"`
	CreatedAt  string  `json:"created_at"`
}
type Visited struct {
	Visited bool   `json:"visited"`
	ID      *int64 `json:"id,omitempty"`
}
type CreatedID struct {
	Message string `json:"message"`
	ID      int64  `json:"id"`
}
type ID struct {
	ID int64 `json:"id"`
}
type GymVisit struct {
	ID        int64     `json:"id"`
	CreatedAt time.Time `json:"created_at"`
}
type ExerciseSetInput struct {
	Reps   int      `json:"reps"`
	Weight *float64 `json:"weight"`
}
type ExerciseInput struct {
	Name string             `json:"name"`
	Sets []ExerciseSetInput `json:"sets"`
}
type ExerciseSet struct {
	ID        int64    `json:"id"`
	SetNumber int      `json:"set_number"`
	Reps      int      `json:"reps"`
	Weight    *float64 `json:"weight"`
}
type Exercise struct {
	ID   int64         `json:"id"`
	Name string        `json:"name"`
	Sets []ExerciseSet `json:"sets"`
}
type LinkTransactionInput struct {
	TransactionID int64 `json:"transaction_id"`
}
type VehicleName struct {
	ID   int64  `json:"id"`
	Name string `json:"name"`
}
type VehicleInput struct {
	Name                     *string  `json:"name"`
	IsActive                 *bool    `json:"is_active"`
	FrontTirePressureSolo    *float64 `json:"front_tire_pressure_solo"`
	RearTirePressureSolo     *float64 `json:"rear_tire_pressure_solo"`
	FrontTirePressurePillion *float64 `json:"front_tire_pressure_pillion"`
	RearTirePressurePillion  *float64 `json:"rear_tire_pressure_pillion"`
	FrontTirePressure        *float64 `json:"front_tire_pressure"`
	RearTirePressure         *float64 `json:"rear_tire_pressure"`
}
type TirePressureInput struct {
	FrontTirePressureSolo    *float64 `json:"front_tire_pressure_solo"`
	RearTirePressureSolo     *float64 `json:"rear_tire_pressure_solo"`
	FrontTirePressurePillion *float64 `json:"front_tire_pressure_pillion"`
	RearTirePressurePillion  *float64 `json:"rear_tire_pressure_pillion"`
	FrontTirePressure        *float64 `json:"front_tire_pressure"`
	RearTirePressure         *float64 `json:"rear_tire_pressure"`
}
type AirFill struct {
	VehicleID int64     `json:"vehicle_id"`
	FilledAt  time.Time `json:"filled_at"`
}
type FuelItemInput struct {
	FuelType  string   `json:"fuel_type" enums:"petrol,diesel,lpg,cng,electric"`
	FillType  string   `json:"fill_type" enums:"full,partial,missed"`
	Quantity  *float64 `json:"quantity"`
	UnitPrice *float64 `json:"unit_price"`
	TotalCost *float64 `json:"total_cost"`
}
type FuelFillupInput struct {
	OdometerKM  float64         `json:"odometer_km"`
	FilledAt    *time.Time      `json:"filled_at"`
	StationName *string         `json:"station_name"`
	Notes       *string         `json:"notes"`
	Items       []FuelItemInput `json:"items"`
}
type FuelFillupCreated struct {
	ID                int64               `json:"id"`
	EconomyKMPerLitre map[string]*float64 `json:"economy_km_per_litre"`
}
type FuelItem struct {
	FuelType  string  `json:"fuel_type"`
	FillType  string  `json:"fill_type"`
	Quantity  float64 `json:"quantity"`
	UnitPrice float64 `json:"unit_price"`
	TotalCost float64 `json:"total_cost"`
}
type FuelFillup struct {
	ID          int64      `json:"id"`
	OdometerKM  float64    `json:"odometer_km"`
	FilledAt    time.Time  `json:"filled_at"`
	StationName *string    `json:"station_name"`
	Notes       *string    `json:"notes"`
	Items       []FuelItem `json:"items"`
}
type AirFillHistory struct {
	ID       int64     `json:"id"`
	FilledAt time.Time `json:"filled_at"`
}
type MaintenanceInput struct {
	Category     string     `json:"category" enums:"service,repair,insurance,washing,tyres"`
	Title        string     `json:"title"`
	Amount       float64    `json:"amount"`
	OccurredAt   *time.Time `json:"occurred_at"`
	OdometerKM   *float64   `json:"odometer_km"`
	ProviderName *string    `json:"provider_name"`
	Notes        *string    `json:"notes"`
}
type Attachment struct {
	ID          int64     `json:"id"`
	FileName    string    `json:"file_name"`
	ContentType string    `json:"content_type"`
	SizeBytes   int64     `json:"size_bytes"`
	CreatedAt   time.Time `json:"created_at"`
}
type MaintenanceRecord struct {
	ID           int64        `json:"id"`
	Category     string       `json:"category"`
	Title        string       `json:"title"`
	Amount       float64      `json:"amount"`
	OccurredAt   time.Time    `json:"occurred_at"`
	OdometerKM   *float64     `json:"odometer_km"`
	ProviderName *string      `json:"provider_name"`
	Notes        *string      `json:"notes"`
	Attachments  []Attachment `json:"attachments"`
}
type VehicleHistory struct {
	VehicleName              string              `json:"vehicle_name"`
	FrontTirePressureSolo    *float64            `json:"front_tire_pressure_solo"`
	RearTirePressureSolo     *float64            `json:"rear_tire_pressure_solo"`
	FrontTirePressurePillion *float64            `json:"front_tire_pressure_pillion"`
	RearTirePressurePillion  *float64            `json:"rear_tire_pressure_pillion"`
	FrontTirePressure        *float64            `json:"front_tire_pressure"`
	RearTirePressure         *float64            `json:"rear_tire_pressure"`
	AirFills                 []AirFillHistory    `json:"air_fills"`
	FuelFillups              []FuelFillup        `json:"fuel_fillups"`
	MaintenanceRecords       []MaintenanceRecord `json:"maintenance_records"`
	AverageMileageKMPerLitre map[string]float64  `json:"average_mileage_km_per_litre"`
}

// Operations are documented here because the router delegates to several packages.
// operation01 documents POST /api/auth/login.
// @Summary Log in
// @Description Sets the life_session HTTP-only cookie.
// @Tags auth
// @Accept json
// @Produce json
// @Param body body LoginInput true "Request body"
// @Success 200 {object} Username
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/auth/login [post]
func operation01() {}

// operation02 documents GET /api/auth/session.
// @Summary Get current session
// @Tags auth
// @Produce json
// @Security SessionBearer
// @Success 200 {object} Username
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/auth/session [get]
func operation02() {}

// operation03 documents POST /api/auth/logout.
// @Summary Log out
// @Description Clears the session cookie and invalidates a supplied session token.
// @Tags auth
// @Produce json
// @Success 200 {object} Message
// @Failure 500 {object} Error
// @Router /api/auth/logout [post]
func operation03() {}

// operation04 documents GET /api/profile.
// @Summary Get profile
// @Tags profile
// @Produce json
// @Security SessionBearer
// @Success 200 {object} Profile
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/profile [get]
func operation04() {}

// operation05 documents PUT /api/profile.
// @Summary Save profile
// @Tags profile
// @Accept json
// @Produce json
// @Param body body Name true "Request body"
// @Security SessionBearer
// @Success 200 {object} Name
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/profile [put]
func operation05() {}

// operation06 documents PUT /api/profile/password.
// @Summary Change password
// @Tags profile
// @Accept json
// @Produce json
// @Param body body ChangePasswordInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} Message
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/profile/password [put]
func operation06() {}

// operation07 documents POST /api/meditation/today.
// @Summary Record today's meditation
// @Tags activity
// @Produce json
// @Success 201 {object} Message
// @Failure 400 {object} Error
// @Failure 500 {object} Error
// @Router /api/meditation/today [post]
func operation07() {}

// operation08 documents POST /api/sport/today.
// @Summary Record today's sport
// @Tags activity
// @Accept json
// @Produce json
// @Param body body SportInput true "Request body"
// @Success 201 {object} Message
// @Failure 400 {object} Error
// @Failure 500 {object} Error
// @Router /api/sport/today [post]
func operation08() {}

// operation09 documents GET /api/next-month-purchases.
// @Summary List next-month purchases
// @Tags purchases
// @Produce json
// @Security SessionBearer
// @Success 200 {object} PurchaseList
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/next-month-purchases [get]
func operation09() {}

// operation10 documents POST /api/next-month-purchases.
// @Summary Create next-month purchase
// @Tags purchases
// @Accept json
// @Produce json
// @Param body body PurchaseInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} PurchaseItem
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/next-month-purchases [post]
func operation10() {}

// operation11 documents DELETE /api/next-month-purchases.
// @Summary Clear next-month purchases
// @Tags purchases
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/next-month-purchases [delete]
func operation11() {}

// operation12 documents DELETE /api/next-month-purchases/{id}.
// @Summary Delete next-month purchase
// @Tags purchases
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/next-month-purchases/{id} [delete]
func operation12() {}

// operation13 documents GET /api/notifications.
// @Summary List notifications
// @Tags notifications
// @Produce json
// @Param limit query integer false "Maximum notifications, 1 to 100 (default 100)" default(100) minimum(1) maximum(100)
// @Security SessionBearer
// @Success 200 {array} Notification
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/notifications [get]
func operation13() {}

// operation14 documents DELETE /api/notifications.
// @Summary Clear notifications
// @Tags notifications
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/notifications [delete]
func operation14() {}

// operation15 documents DELETE /api/notifications/{id}.
// @Summary Dismiss notification
// @Tags notifications
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/notifications/{id} [delete]
func operation15() {}

// operation16 documents GET /api/meal-times.
// @Summary List meal times
// @Tags meal plans
// @Produce json
// @Security SessionBearer
// @Success 200 {array} mealplan.TimeDTO
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-times [get]
func operation16() {}

// operation17 documents POST /api/meal-times.
// @Summary Create meal time
// @Description Times accept HH:MM or HH:MM:SS.
// @Tags meal plans
// @Accept json
// @Produce json
// @Param body body mealplan.CreateTimeInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} mealplan.TimeDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-times [post]
func operation17() {}

// operation18 documents DELETE /api/meal-times/{id}.
// @Summary Delete custom meal time
// @Tags meal plans
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-times/{id} [delete]
func operation18() {}

// operation19 documents GET /api/meal-plans.
// @Summary List meal plans
// @Tags meal plans
// @Produce json
// @Param date query string false "Single date YYYY-MM-DD"
// @Param start_date query string false "Range start YYYY-MM-DD"
// @Param end_date query string false "Range end YYYY-MM-DD"
// @Security SessionBearer
// @Success 200 {array} mealplan.PlanDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-plans [get]
func operation19() {}

// operation20 documents POST /api/meal-plans.
// @Summary Create meal plan
// @Tags meal plans
// @Accept json
// @Produce json
// @Param body body mealplan.CreatePlanInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} mealplan.PlanDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-plans [post]
func operation20() {}

// operation21 documents PATCH /api/meal-plans/{id}/consumed.
// @Summary Set meal consumption
// @Tags meal plans
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body mealplan.UpdatePlanConsumedInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} mealplan.PlanDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-plans/{id}/consumed [patch]
func operation21() {}

// operation22 documents PATCH /api/meal-plans/{id}.
// @Summary Update meal plan
// @Tags meal plans
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body mealplan.UpdatePlanInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} mealplan.PlanDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-plans/{id} [patch]
func operation22() {}

// operation23 documents PUT /api/meal-plans/{id}.
// @Summary Update meal plan
// @Tags meal plans
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body mealplan.UpdatePlanInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} mealplan.PlanDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-plans/{id} [put]
func operation23() {}

// operation24 documents DELETE /api/meal-plans/{id}.
// @Summary Delete meal plan
// @Tags meal plans
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {object} Error
// @Failure 500 {object} Error
// @Router /api/meal-plans/{id} [delete]
func operation24() {}

// operation25 documents GET /api/workout/today.
// @Summary Get today's workout visit
// @Tags gym
// @Produce json
// @Success 200 {object} Visited
// @Failure 500 {object} Error
// @Router /api/workout/today [get]
func operation25() {}

// operation26 documents POST /api/workout/today.
// @Summary Record today's workout visit
// @Tags gym
// @Produce json
// @Success 201 {object} CreatedID
// @Failure 400 {object} Error
// @Failure 500 {object} Error
// @Router /api/workout/today [post]
func operation26() {}

// operation27 documents GET /api/gym-visits.
// @Summary List gym visits
// @Tags gym
// @Produce json
// @Success 200 {array} GymVisit
// @Failure 500 {object} Error
// @Router /api/gym-visits [get]
func operation27() {}

// operation28 documents DELETE /api/gym-visits/{id}.
// @Summary Delete gym visit
// @Tags gym
// @Param id path integer true "Positive id" minimum(1)
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/gym-visits/{id} [delete]
func operation28() {}

// operation29 documents GET /api/gym-visits/{id}/exercises.
// @Summary List visit exercises
// @Tags gym
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Success 200 {array} Exercise
// @Failure 400 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/gym-visits/{id}/exercises [get]
func operation29() {}

// operation30 documents POST /api/gym-visits/{id}/exercises.
// @Summary Add visit exercise
// @Tags gym
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body ExerciseInput true "Request body"
// @Success 201 {object} Exercise
// @Failure 400 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/gym-visits/{id}/exercises [post]
func operation30() {}

// operation31 documents POST /api/notifications/{id}/gym-visit.
// @Summary Mark gym reminder visited
// @Tags gym
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 201 {object} ID
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/notifications/{id}/gym-visit [post]
func operation31() {}

// operation32 documents GET /api/horizon.
// @Summary Get financial summary
// @Tags horizon
// @Produce json
// @Security SessionBearer
// @Success 200 {object} horizon.SummaryDTO
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon [get]
func operation32() {}

// operation33 documents PUT /api/horizon/config.
// @Summary Update financial config
// @Tags horizon
// @Accept json
// @Produce json
// @Param body body horizon.ConfigInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} horizon.SummaryDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/config [put]
func operation33() {}

// operation34 documents POST /api/horizon/budgets.
// @Summary Create budget
// @Tags horizon
// @Accept json
// @Produce json
// @Param body body horizon.BudgetInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} horizon.BudgetDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/budgets [post]
func operation34() {}

// operation35 documents PUT /api/horizon/budgets/{id}.
// @Summary Update budget
// @Tags horizon
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body horizon.BudgetInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} horizon.BudgetDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/budgets/{id} [put]
func operation35() {}

// operation36 documents DELETE /api/horizon/budgets/{id}.
// @Summary Delete budget
// @Tags horizon
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/budgets/{id} [delete]
func operation36() {}

// operation37 documents POST /api/horizon/deductions.
// @Summary Create deduction
// @Tags horizon
// @Accept json
// @Produce json
// @Param body body horizon.DeductionInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} horizon.DeductionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/deductions [post]
func operation37() {}

// operation38 documents PUT /api/horizon/deductions/{id}.
// @Summary Update deduction
// @Tags horizon
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body horizon.DeductionInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} horizon.DeductionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/deductions/{id} [put]
func operation38() {}

// operation39 documents DELETE /api/horizon/deductions/{id}.
// @Summary Delete deduction
// @Tags horizon
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/deductions/{id} [delete]
func operation39() {}

// operation40 documents GET /api/horizon/categories.
// @Summary List categories
// @Tags horizon
// @Produce json
// @Security SessionBearer
// @Success 200 {array} horizon.CategoryDTO
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/categories [get]
func operation40() {}

// operation41 documents POST /api/horizon/categories.
// @Summary Create category
// @Tags horizon
// @Accept json
// @Produce json
// @Param body body horizon.CategoryInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} horizon.CategoryDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/categories [post]
func operation41() {}

// operation42 documents GET /api/horizon/transactions.
// @Summary List transactions
// @Tags horizon
// @Produce json
// @Param page query integer false "Page number" default(1) minimum(1)
// @Param page_size query integer false "Items per page, capped at 100" default(10) minimum(1) maximum(100)
// @Param type query string false "Transaction type" Enums(debit,credit)
// @Param search query string false "Case-insensitive search text"
// @Security SessionBearer
// @Success 200 {object} horizon.PaginatedTransactionsDTO
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/transactions [get]
func operation42() {}

// operation43 documents POST /api/horizon/transactions.
// @Summary Create transaction
// @Tags horizon
// @Accept json
// @Produce json
// @Param body body horizon.TransactionInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} horizon.TransactionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/transactions [post]
func operation43() {}

// operation44 documents POST /api/horizon/transactions/bulk.
// @Summary Create transactions in bulk
// @Tags horizon
// @Accept json
// @Produce json
// @Param body body horizon.BulkTransactionsInput true "Request body"
// @Security SessionBearer
// @Success 201 {array} horizon.TransactionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/transactions/bulk [post]
func operation44() {}

// operation45 documents PUT /api/horizon/transactions/{id}.
// @Summary Update transaction
// @Tags horizon
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body horizon.TransactionInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} horizon.TransactionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/transactions/{id} [put]
func operation45() {}

// operation46 documents DELETE /api/horizon/transactions/{id}.
// @Summary Delete transaction
// @Tags horizon
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/transactions/{id} [delete]
func operation46() {}

// operation47 documents GET /api/horizon/subscriptions.
// @Summary List subscriptions
// @Tags horizon
// @Produce json
// @Security SessionBearer
// @Success 200 {object} horizon.SubscriptionSummaryDTO
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/subscriptions [get]
func operation47() {}

// operation48 documents POST /api/horizon/subscriptions.
// @Summary Create subscription
// @Tags horizon
// @Accept json
// @Produce json
// @Param body body horizon.SubscriptionInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} horizon.SubscriptionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/subscriptions [post]
func operation48() {}

// operation49 documents PUT /api/horizon/subscriptions/{id}.
// @Summary Update subscription
// @Tags horizon
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body horizon.SubscriptionInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} horizon.SubscriptionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/subscriptions/{id} [put]
func operation49() {}

// operation50 documents DELETE /api/horizon/subscriptions/{id}.
// @Summary Delete subscription
// @Tags horizon
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/subscriptions/{id} [delete]
func operation50() {}

// operation51 documents GET /api/horizon/subscriptions/{id}/transactions.
// @Summary List subscription transactions
// @Tags horizon
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 200 {array} horizon.TransactionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/horizon/subscriptions/{id}/transactions [get]
func operation51() {}

// operation52 documents POST /api/horizon/subscriptions/{id}/transactions.
// @Summary Link subscription transaction
// @Tags horizon
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body LinkTransactionInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} horizon.TransactionDTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/horizon/subscriptions/{id}/transactions [post]
func operation52() {}

// operation53 documents GET /api/vehicles.
// @Summary List vehicles
// @Tags vehicles
// @Produce json
// @Success 200 {array} VehicleName
// @Failure 500 {object} Error
// @Router /api/vehicles [get]
func operation53() {}

// operation54 documents POST /api/vehicles.
// @Summary Create vehicle
// @Tags vehicles
// @Accept json
// @Produce json
// @Param body body VehicleInput true "Request body"
// @Success 201 {object} vehicle.DTO
// @Failure 400 {object} Error
// @Failure 500 {object} Error
// @Router /api/vehicles [post]
func operation54() {}

// operation55 documents GET /api/vehicle-air-fills/latest.
// @Summary List latest air fills
// @Tags vehicles
// @Produce json
// @Security SessionBearer
// @Success 200 {array} AirFill
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/vehicle-air-fills/latest [get]
func operation55() {}

// operation56 documents GET /api/vehicles/{id}.
// @Summary Get vehicle
// @Tags vehicles
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Success 200 {object} vehicle.DTO
// @Failure 400 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id} [get]
func operation56() {}

// operation57 documents PUT /api/vehicles/{id}.
// @Summary Update vehicle
// @Tags vehicles
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body VehicleInput true "Request body"
// @Success 200 {object} vehicle.DTO
// @Failure 400 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id} [put]
func operation57() {}

// operation58 documents PATCH /api/vehicles/{id}.
// @Summary Update vehicle
// @Tags vehicles
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body VehicleInput true "Request body"
// @Success 200 {object} vehicle.DTO
// @Failure 400 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id} [patch]
func operation58() {}

// operation59 documents PUT /api/vehicles/{id}/tire-pressure.
// @Summary Update tire pressure
// @Tags vehicles
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body TirePressureInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} vehicle.DTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/tire-pressure [put]
func operation59() {}

// operation60 documents PATCH /api/vehicles/{id}/tire-pressure.
// @Summary Update tire pressure
// @Tags vehicles
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body TirePressureInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} vehicle.DTO
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/tire-pressure [patch]
func operation60() {}

// operation61 documents DELETE /api/vehicles/{id}.
// @Summary Delete vehicle
// @Tags vehicles
// @Param id path integer true "Positive id" minimum(1)
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id} [delete]
func operation61() {}

// operation62 documents POST /api/vehicles/{id}/air-fills.
// @Summary Record air fill
// @Tags vehicles
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 201 {object} AirFill
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/air-fills [post]
func operation62() {}

// operation63 documents POST /api/vehicles/{id}/fuel-fillups.
// @Summary Record fuel fill-up
// @Tags vehicles
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body FuelFillupInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} FuelFillupCreated
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/fuel-fillups [post]
func operation63() {}

// operation64 documents PUT /api/vehicles/{id}/fuel-fillups/{fillupID}.
// @Summary Update fuel fill-up
// @Tags vehicles
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param fillupID path integer true "Positive fillupID" minimum(1)
// @Param body body FuelFillupInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} ID
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/fuel-fillups/{fillupID} [put]
func operation64() {}

// operation65 documents POST /api/vehicles/{id}/maintenance-records.
// @Summary Create maintenance record
// @Tags vehicles
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param body body MaintenanceInput true "Request body"
// @Security SessionBearer
// @Success 201 {object} MaintenanceRecord
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/maintenance-records [post]
func operation65() {}

// operation66 documents PUT /api/vehicles/{id}/maintenance-records/{recordID}.
// @Summary Update maintenance record
// @Tags vehicles
// @Accept json
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param recordID path integer true "Positive recordID" minimum(1)
// @Param body body MaintenanceInput true "Request body"
// @Security SessionBearer
// @Success 200 {object} MaintenanceRecord
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/maintenance-records/{recordID} [put]
func operation66() {}

// operation67 documents POST /api/vehicles/{id}/maintenance-records/{recordID}/attachments.
// @Summary Upload maintenance attachment
// @Description Multipart upload; file must be 1 byte to 10 MB.
// @Tags vehicles
// @Accept multipart/form-data
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Param recordID path integer true "Positive recordID" minimum(1)
// @Param file formData file true "Attachment file"
// @Security SessionBearer
// @Success 201 {object} Attachment
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/maintenance-records/{recordID}/attachments [post]
func operation67() {}

// operation68 documents GET /api/vehicles/{id}/maintenance-records/{recordID}/attachments/{attachmentID}.
// @Summary Download maintenance attachment
// @Description Redirects to a short-lived download URL.
// @Tags vehicles
// @Param id path integer true "Positive id" minimum(1)
// @Param recordID path integer true "Positive recordID" minimum(1)
// @Param attachmentID path integer true "Positive attachmentID" minimum(1)
// @Security SessionBearer
// @Success 302 "Redirect"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/maintenance-records/{recordID}/attachments/{attachmentID} [get]
func operation68() {}

// operation69 documents GET /api/vehicles/{id}/history.
// @Summary Get vehicle history
// @Tags vehicles
// @Produce json
// @Param id path integer true "Positive id" minimum(1)
// @Security SessionBearer
// @Success 200 {object} VehicleHistory
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/history [get]
func operation69() {}

// operation70 documents DELETE /api/vehicles/{id}/air-fills/{airFillID}.
// @Summary Delete air fill
// @Tags vehicles
// @Param id path integer true "Positive id" minimum(1)
// @Param airFillID path integer true "Positive airFillID" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/air-fills/{airFillID} [delete]
func operation70() {}

// operation71 documents DELETE /api/vehicles/{id}/fuel-fillups/{fillupID}.
// @Summary Delete fuel fill-up
// @Tags vehicles
// @Param id path integer true "Positive id" minimum(1)
// @Param fillupID path integer true "Positive fillupID" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/fuel-fillups/{fillupID} [delete]
func operation71() {}

// operation72 documents DELETE /api/vehicles/{id}/maintenance-records/{recordID}.
// @Summary Delete maintenance record
// @Tags vehicles
// @Param id path integer true "Positive id" minimum(1)
// @Param recordID path integer true "Positive recordID" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/maintenance-records/{recordID} [delete]
func operation72() {}

// operation73 documents DELETE /api/vehicles/{id}/maintenance-records/{recordID}/attachments/{attachmentID}.
// @Summary Delete maintenance attachment
// @Tags vehicles
// @Param id path integer true "Positive id" minimum(1)
// @Param recordID path integer true "Positive recordID" minimum(1)
// @Param attachmentID path integer true "Positive attachmentID" minimum(1)
// @Security SessionBearer
// @Success 204 "No Content"
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 404 {string} string
// @Failure 500 {object} Error
// @Router /api/vehicles/{id}/maintenance-records/{recordID}/attachments/{attachmentID} [delete]
func operation73() {}

// operation74 documents GET /metrics.
// @Summary Get Prometheus metrics
// @Description Prometheus text exposition format.
// @Tags operations
// @Produce text/plain
// @Success 200 {string} string
// @Failure 500 {object} Error
// @Router /metrics [get]
func operation74() {}

// operation75 documents GET /swagger/doc.json.
// @Summary Get Swagger document
// @Description Generated Swagger 2.0 document.
// @Tags operations
// @Produce json
// @Success 200 {object} map[string]interface{}
// @Router /swagger/doc.json [get]
func operation75() {}

// operation76 documents GET /swagger/index.html.
// @Summary Open Swagger UI
// @Description Interactive API documentation.
// @Tags operations
// @Produce text/html
// @Success 200 {string} string
// @Router /swagger/index.html [get]
func operation76() {}

// profileOperation1 documents the optional profiler route.
// @Summary Profiler index
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce text/html
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/ [get]
func profileOperation1() {}

// profileOperation2 documents the optional profiler route.
// @Summary Process command line
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/cmdline [get]
func profileOperation2() {}

// profileOperation3 documents the optional profiler route.
// @Summary CPU profile
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Param seconds query integer false "Duration in seconds" minimum(1)
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/profile [get]
func profileOperation3() {}

// profileOperation4 documents the optional profiler route.
// @Summary Symbol lookup
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce text/plain
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/symbol [get]
func profileOperation4() {}

// profileOperation5 documents the optional profiler route.
// @Summary Symbol lookup
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce text/plain
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/symbol [post]
func profileOperation5() {}

// profileOperation6 documents the optional profiler route.
// @Summary Execution trace
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Param seconds query integer false "Duration in seconds" minimum(1)
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/trace [get]
func profileOperation6() {}

// profileOperation7 documents the optional profiler route.
// @Summary Goroutine profile
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/goroutine [get]
func profileOperation7() {}

// profileOperation8 documents the optional profiler route.
// @Summary Heap profile
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/heap [get]
func profileOperation8() {}

// profileOperation9 documents the optional profiler route.
// @Summary Thread creation profile
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/threadcreate [get]
func profileOperation9() {}

// profileOperation10 documents the optional profiler route.
// @Summary Block profile
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/block [get]
func profileOperation10() {}

// profileOperation11 documents the optional profiler route.
// @Summary Mutex profile
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/mutex [get]
func profileOperation11() {}

// profileOperation12 documents the optional profiler route.
// @Summary Allocations profile
// @Description Available only when pprof is enabled. HTTP Basic Auth is required when profiler credentials are configured.
// @Tags operations
// @Produce application/octet-stream
// @Success 200 {string} string
// @Failure 401 {string} string
// @Router /debug/pprof/allocs [get]
func profileOperation12() {}
