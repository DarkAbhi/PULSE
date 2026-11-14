// Test-only contract server. Real persistence and transaction behavior are covered
// separately by the Go suite against disposable PostgreSQL containers.
import http from "node:http";
const states = new Map();
const failures = new Map();
const writes = new Map();
const customTimes = new Map();
let nextTimeId = 5;
const pending = () => ({ status: "pending", questionnaire_version: 1, preferences: null });
const times = [
  { id: 1, name: "Breakfast", start_time: "07:00", end_time: "10:00", is_default: true },
  { id: 2, name: "Lunch", start_time: "12:00", end_time: "14:30", is_default: true },
  { id: 3, name: "Evening Snacks", start_time: "16:30", end_time: "18:30", is_default: true },
  { id: 4, name: "Dinner", start_time: "19:30", end_time: "22:00", is_default: true },
];
http.createServer(async (req, res) => {
  const url = new URL(req.url, "http://127.0.0.1:18081");
  const token = req.headers.authorization?.replace("Bearer ", "") ?? url.searchParams.get("user") ?? "anonymous";
  const send = (status, body) => { res.writeHead(status, { "Content-Type": "application/json", "Cache-Control": "no-store" }); res.end(JSON.stringify(body)); };
  const chunks = []; for await (const chunk of req) chunks.push(chunk);
  let body; try { body = chunks.length ? JSON.parse(Buffer.concat(chunks)) : {}; } catch { send(400, { error: "Invalid JSON" }); return; }
  if (url.pathname === "/health") return send(200, {});
  if (url.pathname === "/__test/fail") { failures.set(token, body.mode); return send(200, {}); }
  if (url.pathname === "/__test/state") return send(200, { state: states.get(token) ?? pending(), writes: writes.get(token) ?? 0 });
  if (url.pathname === "/api/auth/session") return send(200, { username: token });
  if (url.pathname === "/api/profile") return send(200, { name: "Browser test", timezone: "Asia/Kolkata", currency: "INR" });
  if (url.pathname === "/api/meal-times") {
    const existing = customTimes.get(token) ?? [];
    if (req.method === "GET") return send(200, [...times, ...existing]);
    if (req.method === "POST") {
      const time = { id: nextTimeId++, is_default: false, ...body };
      customTimes.set(token, [...existing, time]);
      return send(201, time);
    }
  }
  if (url.pathname.startsWith("/api/meal-times/") && req.method === "DELETE") {
    const id = Number(url.pathname.split("/").at(-1));
    customTimes.set(token, (customTimes.get(token) ?? []).filter(time => time.id !== id));
    res.writeHead(204).end();
    return;
  }
  if (url.pathname === "/api/meal-plans") return send(200, []);
  if (url.pathname === "/api/meal-plan-onboarding" && req.method === "GET") {
    if (failures.get(token) === "read") return send(503, { error: "Unable to load preferences" });
    return send(200, states.get(token) ?? pending());
  }
  if (["/api/meal-plan-onboarding", "/api/meal-preferences"].includes(url.pathname) && req.method === "PUT") {
    if (failures.get(token) === "write") return send(503, { error: "Test connection interrupted. Please try again." });
    const current = states.get(token) ?? pending();
    if (url.pathname.endsWith("onboarding") && current.status !== "pending") return send(current.status === body.status ? 200 : 409, current.status === body.status ? current : { error: "Already resolved", state: current });
    const state = url.pathname.endsWith("onboarding") ? { ...pending(), status: body.status, decided_at: new Date().toISOString(), preferences: body.preferences ?? null } : { ...current, preferences: body };
    states.set(token, state); writes.set(token, (writes.get(token) ?? 0) + 1);
    if (failures.get(token) === "lost-response") { failures.delete(token); req.socket.destroy(); return; }
    return send(200, state);
  }
  send(404, { error: "Unknown fixture endpoint" });
}).listen(18081, "127.0.0.1");
