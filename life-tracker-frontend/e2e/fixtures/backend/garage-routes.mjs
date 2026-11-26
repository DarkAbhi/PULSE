// Garage vehicle history.

/** Answers the request and returns true, or returns undefined to let the next domain try. */
export function route({ req, res, url, token, body, send }) {
  if (url.pathname === "/api/vehicles/1/history") {
    return send(200, {
      vehicle_name: "Test motorcycle", kind: "motorcycle", fuel_types: ["petrol"],
      fuel_fillups: [], air_fills: [], maintenance_records: [],
      maintenance_schedules: [{
        kind: "engine_oil", active_source: "personal",
        personal: { kilometers: 1000, duration: 6 },
        manual: { kilometers: 0, duration: 0 },
        manual_reference: "",
      }],
    });
  }
}
