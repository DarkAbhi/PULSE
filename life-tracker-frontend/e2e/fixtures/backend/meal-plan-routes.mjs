// Meal times, recipes, meal plans and onboarding, plus /__test controls for onboarding state and failures.
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
let nextTimeId = 5;
const pending = () => ({ status: "pending", questionnaire_version: 1, preferences: null });

/** Restores this domain's data for one session so every test starts from the same state. */
export function reset(token) {
  for (const byUser of [states, failures, writes, customTimes, mealsByUser]) byUser.delete(token);
}
const times = [
  { id: 1, name: "Breakfast", start_time: "07:00", end_time: "10:00", is_default: true },
  { id: 2, name: "Lunch", start_time: "12:00", end_time: "14:30", is_default: true },
  { id: 3, name: "Evening Snacks", start_time: "16:30", end_time: "18:30", is_default: true },
  { id: 4, name: "Dinner", start_time: "19:30", end_time: "22:00", is_default: true },
];

/** Answers the request and returns true, or returns undefined to let the next domain try. */
export function route({ req, res, url, token, body, send }) {
  if (url.pathname === "/__test/fail") { failures.set(token, body.mode); return send(200, {}); }
  if (url.pathname === "/__test/state") return send(200, { state: states.get(token) ?? pending(), writes: writes.get(token) ?? 0 });
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
    return true;
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
    if (failures.get(token) === "lost-response") { failures.delete(token); req.socket.destroy(); return true; }
    return send(200, state);
  }
}
