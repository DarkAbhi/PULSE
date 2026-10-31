package vehicle

import (
	"net/http"
	"strings"

	"github.com/DarkAbhi/life-backend/internal/webutil"
)

type CatalogOption struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type CatalogMatch struct {
	ID        string `json:"id"`
	MakeSlug  string `json:"make_slug"`
	MakeName  string `json:"make_name"`
	ModelName string `json:"model_name"`
}

type CatalogMatchResponse struct {
	Match *CatalogMatch `json:"match"`
}

// CatalogMatch suggests only a unique exact match; it never changes a vehicle.
func (h *Handler) CatalogMatch(w http.ResponseWriter, r *http.Request) {
	if _, ok := h.maintenanceUser(w, r); !ok {
		return
	}
	kind := r.URL.Query().Get("kind")
	makeName := strings.TrimSpace(r.URL.Query().Get("make_name"))
	modelName := strings.TrimSpace(r.URL.Query().Get("model_name"))
	if !validKind(kind) || makeName == "" || modelName == "" || len([]rune(makeName)) > 200 || len([]rune(modelName)) > 200 {
		webutil.BadRequest(w, "choose a valid kind and provide make_name and model_name of at most 200 characters")
		return
	}
	rows, err := h.DB.Query(r.Context(), `SELECT id, make_slug, make_name, model_name FROM vehicle_catalog
		WHERE kind=$1 AND lower(btrim(make_name))=lower($2) AND lower(btrim(model_name))=lower($3)
		ORDER BY id LIMIT 2`, kind, makeName, modelName)
	if err != nil {
		h.writeError(w, r, err)
		return
	}
	defer rows.Close()
	matches := []CatalogMatch{}
	for rows.Next() {
		var match CatalogMatch
		if err := rows.Scan(&match.ID, &match.MakeSlug, &match.MakeName, &match.ModelName); err != nil {
			h.writeError(w, r, err)
			return
		}
		matches = append(matches, match)
	}
	if err := rows.Err(); err != nil {
		h.writeError(w, r, err)
		return
	}
	var match *CatalogMatch
	if len(matches) == 1 {
		match = &matches[0]
	}
	webutil.WriteJSON(w, http.StatusOK, CatalogMatchResponse{Match: match})
}

// CatalogOptions serves bounded, parameterized searches against the local snapshot.
func (h *Handler) CatalogOptions(w http.ResponseWriter, r *http.Request) {
	if _, ok := h.maintenanceUser(w, r); !ok {
		return
	}
	if strings.HasSuffix(r.URL.Path, "/kinds") {
		webutil.WriteJSON(w, http.StatusOK, []CatalogOption{
			{"car", "Car"}, {"motorcycle", "Motorcycle"}, {"moped", "Moped"},
			{"van", "Van"}, {"truck", "Truck"}, {"bus", "Bus"},
		})
		return
	}
	kind := r.URL.Query().Get("kind")
	if !validKind(kind) {
		webutil.BadRequest(w, "choose a valid vehicle kind")
		return
	}
	search := strings.TrimSpace(r.URL.Query().Get("q"))
	if len([]rune(search)) > 100 {
		webutil.BadRequest(w, "search must be at most 100 characters")
		return
	}
	// Treat search text literally, including SQL LIKE wildcard characters.
	search = "%" + strings.NewReplacer("\\", "\\\\", "%", "\\%", "_", "\\_").Replace(strings.ToLower(search)) + "%"
	makeSlug := r.URL.Query().Get("make_slug")
	query := `SELECT make_slug, min(make_name) FROM vehicle_catalog
        WHERE kind=$1 AND lower(make_name) LIKE $2 GROUP BY make_slug ORDER BY min(make_name), make_slug LIMIT 100`
	args := []any{kind, search}
	if strings.HasSuffix(r.URL.Path, "/models") {
		if makeSlug == "" || len(makeSlug) > 200 {
			webutil.BadRequest(w, "make_slug is required")
			return
		}
		query = `SELECT id, model_name FROM vehicle_catalog
            WHERE kind=$1 AND lower(make_name || ' ' || model_name || ' ' || aliases) LIKE $2 AND make_slug=$3
            ORDER BY model_name, id LIMIT 100`
		args = append(args, makeSlug)
	}
	rows, err := h.DB.Query(r.Context(), query, args...)
	if err != nil {
		h.writeError(w, r, err)
		return
	}
	defer rows.Close()
	options := []CatalogOption{}
	for rows.Next() {
		var option CatalogOption
		if err := rows.Scan(&option.ID, &option.Name); err != nil {
			h.writeError(w, r, err)
			return
		}
		options = append(options, option)
	}
	if err := rows.Err(); err != nil {
		h.writeError(w, r, err)
		return
	}
	webutil.WriteJSON(w, http.StatusOK, options)
}
