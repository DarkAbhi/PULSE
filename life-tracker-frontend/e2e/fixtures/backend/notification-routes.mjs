// Notification center.
const dismissedNotifications = new Set();

/** Restores this domain's data for one session so every test starts from the same state. */
export function reset(token) {
  dismissedNotifications.delete(token);
}

/** Answers the request and returns true, or returns undefined to let the next domain try. */
export function route({ req, res, url, token, body, send }) {
  if (url.pathname === "/api/notifications") {
    return send(200, token === "app-bar-test" && !dismissedNotifications.has(token) ? [{
      id: 1, title: "Test reminder", body: "Check tire pressure", source: "garage",
      target_path: null, priority: 0, created_at: "2026-01-02T10:00:00Z",
    }] : []);
  }
  if (url.pathname === "/api/notifications/1" && req.method === "DELETE" && token === "app-bar-test") {
    dismissedNotifications.add(token);
    res.writeHead(204).end();
    return true;
  }
}
