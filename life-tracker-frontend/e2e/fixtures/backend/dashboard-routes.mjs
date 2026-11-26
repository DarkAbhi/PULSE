// Dashboard cards: today's activity, quest activity, recent activity and today's workout.

/** Answers the request and returns true, or returns undefined to let the next domain try. */
export function route({ req, res, url, token, body, send }) {
  if (token === "dashboard-activity-test") {
    if (url.pathname === "/api/fitness/today") {
      return send(200, {
        date: "2026-01-06", steps_count: 4000, move_calories: 312, move_calories_goal: 300,
        exercise_minutes: 20, exercise_minutes_goal: 30, stand_hours: 9, stand_hours_goal: 12,
        total_asleep_seconds: 26400,
      });
    }
    if (url.pathname === "/api/quests/settings") {
      return send(200, { settings_effective: "2026-01-02", settings: { step_target: 5000 } });
    }
    if (url.pathname === "/api/quests/activity") {
      const quests = ["gym_workout", "daily_steps", "activity_rings", "sleep_goal", "daily_meals"];
      return send(200, {
        today: "2026-01-06",
        days: [
          { date: "2026-01-06", completed_ids: [quests[2]] },
          { date: "2026-01-05", completed_ids: [quests[0]] },
          { date: "2026-01-04", completed_ids: quests.slice(0, 2) },
          { date: "2026-01-03", completed_ids: quests.slice(0, 4) },
          { date: "2026-01-02", completed_ids: quests.slice(0, 3) },
          { date: "2026-01-01", completed_ids: quests },
        ],
      });
    }
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
      return true;
    }
    return send(200, { visited: false });
  }
}
