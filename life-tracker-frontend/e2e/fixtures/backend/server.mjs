// Test-only contract server for the Playwright suite. Real persistence and transaction behavior
// are covered separately by the Go suite against disposable PostgreSQL containers.
//
// Each domain's routes live in their own module and answer only the session tokens their specs use,
// so specs stay independent. Domains are tried in order; the first to answer wins. Domains with
// mutable data export reset(token), which POST /__test/reset?user=<token> calls for every domain.
import http from "node:http";
import { route as account } from "./account-routes.mjs";
import { route as dashboard } from "./dashboard-routes.mjs";
import { reset as resetFinancialHorizon, route as financialHorizon } from "./financial-horizon-routes.mjs";
import { reset as resetFitness, route as fitness } from "./fitness-routes.mjs";
import { route as garage } from "./garage-routes.mjs";
import { reset as resetMealPlan, route as mealPlan } from "./meal-plan-routes.mjs";
import { reset as resetNotifications, route as notifications } from "./notification-routes.mjs";

const routes = [account, dashboard, notifications, garage, fitness, financialHorizon, mealPlan];
const resets = [resetNotifications, resetFitness, resetFinancialHorizon, resetMealPlan];

http.createServer(async (req, res) => {
  const url = new URL(req.url, "http://127.0.0.1:18081");
  const token = req.headers.authorization?.replace("Bearer ", "") ?? url.searchParams.get("user") ?? "anonymous";
  const send = (status, body) => { res.writeHead(status, { "Content-Type": "application/json", "Cache-Control": "no-store" }); res.end(JSON.stringify(body)); return true; };
  const chunks = []; for await (const chunk of req) chunks.push(chunk);
  let body; try { body = chunks.length ? JSON.parse(Buffer.concat(chunks)) : {}; } catch { send(400, { error: "Invalid JSON" }); return; }
  if (url.pathname === "/health") return send(200, {});
  if (url.pathname === "/__test/reset" && req.method === "POST") {
    for (const reset of resets) reset(token);
    return send(204);
  }
  for (const route of routes) if (route({ req, res, url, token, body, send })) return;
  send(404, { error: "Unknown fixture endpoint" });
}).listen(Number(process.env.PULSE_E2E_API_PORT ?? 18081), "127.0.0.1");
