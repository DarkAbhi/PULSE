package apidoc

// @Summary List vehicle kinds
// @Tags vehicles
// @Produce json
// @Security SessionBearer
// @Success 200 {array} vehicle.CatalogOption
// @Failure 401 {object} Error
// @Router /api/vehicle-catalog/kinds [get]
func catalogKinds() {}

// @Summary Search vehicle makes
// @Description Returns up to 100 matches in name order. Use q to narrow the list.
// @Tags vehicles
// @Produce json
// @Param kind query string true "Vehicle kind" Enums(car,motorcycle,moped,van,truck,bus)
// @Param q query string false "Literal search text, at most 100 characters"
// @Security SessionBearer
// @Success 200 {array} vehicle.CatalogOption
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/vehicle-catalog/makes [get]
func catalogMakes() {}

// @Summary Search vehicle models
// @Description Returns up to 100 matches in name order. Model option IDs are kind/make_slug/model_slug.
// @Tags vehicles
// @Produce json
// @Param kind query string true "Vehicle kind" Enums(car,motorcycle,moped,van,truck,bus)
// @Param make_slug query string true "Make ID returned by the makes endpoint"
// @Param q query string false "Literal search text, at most 100 characters; includes model aliases"
// @Security SessionBearer
// @Success 200 {array} vehicle.CatalogOption
// @Failure 400 {object} Error
// @Failure 401 {object} Error
// @Failure 500 {object} Error
// @Router /api/vehicle-catalog/models [get]
func catalogModels() {}
