package vehicle

import "github.com/go-chi/chi/v5"

func (h *Handler) RegisterRoutes(r chi.Router) {
	r.Get("/vehicles", h.ListVehicles)
	r.Post("/vehicles", h.CreateVehicle)
	r.Get("/vehicle-air-fills/latest", h.ListLatestVehicleAirFills)
	r.Route("/vehicles/{id}", func(v chi.Router) {
		v.Get("/", h.GetVehicle)
		v.Put("/", h.UpdateVehicle)
		v.Patch("/", h.UpdateVehicle)
		v.Put("/tire-pressure", h.UpdateVehicleTirePressure)
		v.Patch("/tire-pressure", h.UpdateVehicleTirePressure)
		v.Delete("/", h.DeleteVehicle)
		v.Post("/air-fills", h.CreateVehicleAirFill)
		v.Post("/fuel-fillups", h.CreateFuelFillup)
		v.Put("/fuel-fillups/{fillupID}", h.UpdateFuelFillup)
		v.Post("/maintenance-records", h.CreateMaintenanceRecord)
		v.Put("/maintenance-records/{recordID}", h.UpdateMaintenanceRecord)
		v.Post("/maintenance-records/{recordID}/attachments", h.CreateMaintenanceAttachment)
		v.Get("/maintenance-records/{recordID}/attachments/{attachmentID}", h.DownloadMaintenanceAttachment)
		v.Get("/history", h.VehicleHistory)
		v.Delete("/air-fills/{airFillID}", h.DeleteVehicleAirFill)
		v.Delete("/fuel-fillups/{fillupID}", h.DeleteFuelFillup)
		v.Delete("/maintenance-records/{recordID}", h.DeleteMaintenanceRecord)
		v.Delete("/maintenance-records/{recordID}/attachments/{attachmentID}", h.DeleteMaintenanceAttachment)
	})
}
