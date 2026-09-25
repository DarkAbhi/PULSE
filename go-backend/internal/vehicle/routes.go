package vehicle

import "github.com/go-chi/chi/v5"

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/vehicles", h.List)
	r.Post("/vehicles", h.Create)
	r.Get("/vehicle-air-fills/latest", h.ListLatestAirFills)
	r.Route("/vehicles/{id}", func(v chi.Router) {
		v.Get("/", h.Show)
		v.Put("/", h.Update)
		v.Patch("/", h.Update)
		v.Put("/tire-pressure", h.UpdateTirePressure)
		v.Patch("/tire-pressure", h.UpdateTirePressure)
		v.Delete("/", h.Delete)
		v.Post("/air-fills", h.CreateAirFill)
		v.Post("/fuel-fillups", h.CreateFuelFillup)
		v.Put("/fuel-fillups/{fillupID}", h.UpdateFuelFillup)
		v.Post("/maintenance-records", h.CreateMaintenanceRecord)
		v.Put("/maintenance-records/{recordID}", h.UpdateMaintenanceRecord)
		v.Post("/maintenance-records/{recordID}/attachments", h.CreateMaintenanceAttachment)
		v.Get("/maintenance-records/{recordID}/attachments/{attachmentID}", h.DownloadMaintenanceAttachment)
		v.Get("/history", h.History)
		v.Delete("/air-fills/{airFillID}", h.DeleteAirFill)
		v.Delete("/fuel-fillups/{fillupID}", h.DeleteFuelFillup)
		v.Delete("/maintenance-records/{recordID}", h.DeleteMaintenanceRecord)
		v.Delete("/maintenance-records/{recordID}/attachments/{attachmentID}", h.DeleteMaintenanceAttachment)
	})
}
