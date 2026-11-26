// Fitness overview, workout schedule and gym visits.
const fitnessTestDay = "2026-01-02";
const fitnessSchedule = new Map();
let fitnessClassification = {};
const initialVisits = () => [{ id: 1, time_label: "7:00 AM" }];
const fitnessTestVisits = initialVisits();

/** Restores this domain's data for one session so every test starts from the same state. */
export function reset(token) {
  if (token === "fitness-repeat-visit") fitnessTestVisits.splice(0, fitnessTestVisits.length, ...initialVisits());
  if (token === "fitness-workout-schedule") {
    fitnessSchedule.clear();
    fitnessClassification = {};
  }
}

/** Answers the request and returns true, or returns undefined to let the next domain try. */
export function route({ req, res, url, token, body, send }) {
  if (url.pathname === "/api/fitness/workout-schedule") {
    return send(200, token === "fitness-workout-schedule" ? [...fitnessSchedule.values()] : []);
  }
  const schedulePath = url.pathname.match(/^\/api\/fitness\/workout-schedule\/([1-7])$/);
  if (schedulePath && req.method === "PUT" && token === "fitness-workout-schedule") {
    const weekday = Number(schedulePath[1]);
    if (body.workout_type) fitnessSchedule.set(weekday, { weekday, ...body });
    else fitnessSchedule.delete(weekday);
    return send(204);
  }
  if (url.pathname === "/api/fitness-workouts/1/classification" && req.method === "PUT" && token === "fitness-workout-schedule") {
    fitnessClassification = { workout_type: body.workout_type || null, workout_focus: body.workout_focus || null };
    return send(204);
  }
  if (url.pathname === "/api/fitness/overview" && ["fitness-repeat-visit", "fitness-workout-schedule"].includes(token)) {
    const visits = token === "fitness-repeat-visit"
      ? fitnessTestVisits
      : [{ ...fitnessTestVisits.find(visit => visit.id === 1), ...fitnessClassification }];
    const group = { date: fitnessTestDay, date_label: "Friday, 2 January 2026", workouts: visits };
    return send(200, {
      today: fitnessTestDay,
      cards: [
        { id: "last_workout", label: "Last worked out", value: "Today", subtitle: "at gym" },
        { id: "streak", label: "Workout streak", value: "1 week", subtitle: "with a gym visit" },
      ],
      calendar_workouts: { [fitnessTestDay]: group },
      recent_workouts: token === "fitness-workout-schedule" ? [group] : [],
    });
  }
  if (url.pathname === "/api/gym-visits" && req.method === "POST" && token === "fitness-repeat-visit") {
    if (body?.date !== fitnessTestDay) return send(400, { error: "Incorrect visit date" });
    const id = fitnessTestVisits.length + 1;
    fitnessTestVisits.unshift({ id, time_label: "5:00 PM" });
    return send(201, { id });
  }
  if (url.pathname === "/api/gym-visits/1/exercises") return send(200, []);
  if (url.pathname === "/api/fitness-workouts/1") {
    return send(200, { id: 1, created_at: "2026-01-02T10:00:00Z", ...(token === "fitness-workout-schedule" ? fitnessClassification : {}) });
  }
}
