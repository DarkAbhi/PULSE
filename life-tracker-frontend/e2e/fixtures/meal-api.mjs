// Test-only contract server. Real persistence and transaction behavior are covered
// separately by the Go suite against disposable PostgreSQL containers.
import http from "node:http";
const states = new Map();
const failures = new Map();
const writes = new Map();
const customTimes = new Map();
const mealsByUser = new Map();
const pongal = {
  id: "fixture-pongal", name: "Ven Pongal", dish_type: "Rice", is_ready_to_eat: false, ready_in_minutes: 30,
  description: "A South Indian breakfast made with rice and moong dal.", alternate_names: ["Khara Pongal"],
  ingredients: [{ name: "Rice", quantity: 1, unit: "Cup", important: true }],
  labels: [{ kind: "meal_time", name: "Breakfast" }],
  steps: [{ id: "cook", name: "Cook the rice and dal", instructions: "Simmer the rice and dal until soft.", ingredients: [], equipment: [], type: "cook_method" }],
};
const accountsByUser = new Map();
const dismissedNotifications = new Set();
const horizonCategories = [
  { id: 1, name: "Test category", allows_subcategories: true, parent_id: null },
  { id: 2, name: "Test detail", allows_subcategories: false, parent_id: 1 },
  { id: 3, name: "Test standalone", allows_subcategories: false, parent_id: null },
  { id: 4, name: "others", allows_subcategories: false, parent_id: 1 },
].map(category => ({ ...category, icon: "tag", color: "#64748b", is_default: true }));
const horizonTransactions = [
  { id: 1, name: "Test parent expense", category_id: 1, category_name: "Test category" },
  { id: 2, name: "Test child expense", category_id: 2, category_name: "Test category / Test detail", source: "statement", original_narration: "UPI/DR/104332516949/ICCL ZER/HDFC/zerodha/" },
  { id: 3, name: "Test standalone expense", category_id: 3, category_name: "Test standalone" },
].map(tx => ({ ...tx, type: "debit", amount: 10, transaction_date: "2026-01-02T10:00:00Z" }));
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
  if (url.pathname === "/api/auth/login" && req.method === "POST") {
    if (body.username !== "dashboard-test" || body.password !== "fixture-password") {
      return send(401, { error: "Invalid username or password" });
    }
    res.setHeader("Set-Cookie", "life_session=dashboard-test; Path=/; HttpOnly; SameSite=Lax");
    return send(200, { username: body.username });
  }
  if (url.pathname === "/api/auth/session") {
    return token === "anonymous" || token === "expired-session"
      ? send(401, { error: "Expired session" })
      : send(200, { username: token });
  }
  if ((token === "anonymous" || token === "expired-session") && url.pathname.startsWith("/api/")) {
    return send(401, { error: "Expired session" });
  }
  if (url.pathname === "/api/profile/api-keys" && req.method === "GET") return send(200, []);
  if (url.pathname === "/api/profile") {
    return send(200, { has_profile: true, name: "Browser test", timezone: "Asia/Kolkata", currency: "INR" });
  }
  if (url.pathname === "/api/activity/recent") {
    if (token === "dashboard-activity-test") {
      const recorded_at = new Date().toISOString();
      return send(200, [
        { id: "3", kind: "fuel_fillup", recorded_at, details: { name: "Himalayan 411", quantity: 12.4, amount: 1478 } },
        { id: "2", kind: "meal_added", recorded_at, details: { name: "Paneer burrito bowl", meal_time: "Lunch" } },
        { id: "1", kind: "statement_imported", recorded_at, details: { name: "HDFC Millennia" } },
      ]);
    }
    return send(200, []);
  }
  if (url.pathname === "/api/workout/today") {
    if (token === "dashboard-missing-workout") {
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end("{");
      return;
    }
    return send(200, { visited: false });
  }
  if (url.pathname === "/api/notifications") {
    return send(200, token === "app-bar-test" && !dismissedNotifications.has(token) ? [{
      id: 1, title: "Test reminder", body: "Check tire pressure", source: "garage",
      target_path: null, priority: 0, created_at: "2026-01-02T10:00:00Z",
    }] : []);
  }
  if (url.pathname === "/api/notifications/1" && req.method === "DELETE" && token === "app-bar-test") {
    dismissedNotifications.add(token);
    res.writeHead(204).end();
    return;
  }
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
  if (url.pathname === "/api/gym-visits/1/exercises") return send(200, []);
  if (url.pathname === "/api/fitness-workouts/1") {
    return send(200, { id: 1, created_at: "2026-01-02T10:00:00Z" });
  }
  if (url.pathname === "/api/horizon") return send(200, {
    base_amount: 0, currency: "INR", total_deductions: 0, total_transactions: 0,
    remaining_amount: 0, committed_ratio: 0, total_budgets_allocated: 0, total_subscription_burn: 0,
    budgets: [], deductions: [], categories: token === "categories-test" ? horizonCategories : [],
    transactions: [], subscriptions: [], projections: [],
  });
  if (url.pathname === "/api/horizon/transactions") {
    const categoryId = Number(url.searchParams.get("category_id"));
    const transactions = token === "categories-test" ? horizonTransactions.filter(tx => {
      const category = horizonCategories.find(item => item.id === tx.category_id);
      return !categoryId || tx.category_id === categoryId || category.parent_id === categoryId;
    }) : [];
    return send(200, {
      transactions, page: 1, page_size: 10, total: transactions.length, total_pages: transactions.length ? 1 : 0,
    });
  }
  if (url.pathname === "/api/next-month-purchases") return send(200, { items: [], total: 0 });
  if (url.pathname === "/api/horizon/accounts") {
    const accounts = accountsByUser.get(token) ?? [{
      id: 1, display_name: "Test savings", institution_name: "Kotak", account_type: "bank_account",
      bank_account_subtype: "savings", last_four: "", archived_at: null, account_number: "", holder_name: "Test Person", holder_email: "test@example.com", card_type: "", card_network: "", credit_limit: null, statement_day: null,
    }];
    if (req.method === "GET") return send(200, { accounts });
    if (req.method === "POST") {
      const account = { ...body, id: accounts.length + 1, archived_at: null };
      accounts.push(account);
      accountsByUser.set(token, accounts);
      return send(201, account);
    }
  }
  const accountPath = url.pathname.match(/^\/api\/horizon\/accounts\/(\d+)(?:\/(archive|restore))?$/);
  if (accountPath) {
    const accounts = accountsByUser.get(token) ?? [];
    const account = accounts.find(item => item.id === Number(accountPath[1]));
    if (!account) return send(404, { error: "Account not found" });
    if (req.method === "PUT") Object.assign(account, body);
    else account.archived_at = accountPath[2] === "archive" ? new Date().toISOString() : null;
    return send(200, account);
  }
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
  if (url.pathname === "/api/meal-recipes") {
    const term = (url.searchParams.get("q") ?? "").toLowerCase();
    const matches = [pongal.name, ...pongal.alternate_names].some(name => name.toLowerCase().includes(term));
    const { id, name, dish_type, is_ready_to_eat, ready_in_minutes } = pongal;
    return send(200, { recipes: matches ? [{ id, name, dish_type, is_ready_to_eat, ready_in_minutes }] : [], has_more: false });
  }
  if (url.pathname === `/api/meal-recipes/${pongal.id}`) return send(200, pongal);
  if (url.pathname === "/api/meal-plans") {
    const meals = mealsByUser.get(token) ?? [];
    if (req.method === "GET") return send(200, meals);
    const time = times.find(item => item.id === body.meal_time_id);
    const meal = {
      ...body, name: body.recipe_id === pongal.id ? pongal.name : body.name, id: meals.length + 1,
      meal_time_name: time?.name, is_consumed: false, created_at: new Date().toISOString(),
    };
    mealsByUser.set(token, [...meals, meal]);
    return send(201, meal);
  }
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
}).listen(Number(process.env.PULSE_E2E_API_PORT ?? 18081), "127.0.0.1");
