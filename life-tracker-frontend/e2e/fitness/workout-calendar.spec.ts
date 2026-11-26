import { expect, test } from "@playwright/test";
import { signIn } from "@/e2e/helpers/session";

test("logs another gym visit on the same day and keeps both workout links", async ({ page, context }) => {
  await signIn(context, "fitness-repeat-visit");
  await page.goto("/fitness");

  const calendar = page.getByRole("region", { name: "Workout calendar" });
  const selectedDay = page.getByRole("region", { name: "Workouts for selected date" });
  const logVisit = calendar.getByRole("button", { name: "Log workout", exact: true });
  const morningVisit = selectedDay.getByRole("link", { name: "View workout for gym visit at 7:00 AM", exact: true });
  await expect(morningVisit).toHaveAttribute("href", "/fitness/1");
  await expect(morningVisit).toHaveText("View workout");
  await expect(selectedDay.getByText("Gym visit at 7:00 AM", { exact: true })).toBeVisible();

  await logVisit.click();
  const dialog = page.getByRole("dialog", { name: "Log workout?" });
  await dialog.getByRole("button", { name: "Log workout", exact: true }).click();
  await expect(dialog).toBeHidden();
  await expect(selectedDay.getByRole("link", { name: "View workout for gym visit at 5:00 PM", exact: true })).toHaveAttribute("href", "/fitness/2");
  await expect(morningVisit).toHaveAttribute("href", "/fitness/1");
  await expect(logVisit).toBeVisible();

  await calendar.getByRole("button", { name: /\b3 January 2026, no workouts/ }).click();
  await expect(logVisit).toBeHidden();
  await expect(selectedDay.getByText("No workout logged", { exact: true })).toBeVisible();
});

test("places selected workouts below the summary on wide and narrow screens", async ({ page, context }) => {
  await signIn(context, "fitness-repeat-visit");
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto("/fitness");

  const calendar = page.getByRole("region", { name: "Workout calendar" });
  const selectedDay = page.getByRole("region", { name: "Workouts for selected date" });
  const last = page.getByRole("heading", { name: "Last worked out" });
  const streak = page.getByRole("heading", { name: "Workout streak" });

  async function positions() {
    const [calendarBox, selectedDayBox, lastBox, streakBox] = await Promise.all([
      calendar.boundingBox(), selectedDay.boundingBox(), last.boundingBox(), streak.boundingBox(),
    ]);
    if (!calendarBox || !selectedDayBox || !lastBox || !streakBox) throw new Error("Missing fitness overview panels");
    return { calendar: calendarBox, selectedDay: selectedDayBox, last: lastBox, streak: streakBox };
  }

  let boxes = await positions();
  expect(boxes.selectedDay.x).toBeGreaterThanOrEqual(boxes.calendar.x + boxes.calendar.width);
  expect(boxes.selectedDay.y).toBeGreaterThan(boxes.streak.y);
  expect(boxes.streak.y).toBeGreaterThan(boxes.last.y);

  await page.setViewportSize({ width: 390, height: 844 });
  boxes = await positions();
  expect(boxes.selectedDay.x).toBe(boxes.calendar.x);
  expect(boxes.last.y).toBeGreaterThanOrEqual(boxes.calendar.y + boxes.calendar.height);
  expect(boxes.selectedDay.y).toBeGreaterThan(boxes.streak.y);
});

test("keeps the recurring plan separate from the completed workout type", async ({ page, context }) => {
  await signIn(context, "fitness-workout-schedule");
  await page.goto("/fitness");

  const schedule = page.getByRole("region", { name: "Workout schedule" });
  const friday = schedule.getByRole("button", { name: /Edit Friday schedule/ });
  await friday.click();
  const scheduleEditor = page.getByRole("dialog", { name: "Edit Friday" });
  await scheduleEditor.getByLabel("Workout type").selectOption("push");
  await scheduleEditor.getByLabel("Focus (optional)").fill("Chest and triceps");
  await scheduleEditor.getByRole("button", { name: "Save" }).click();
  await expect(scheduleEditor).toBeHidden();
  await expect(friday).toContainText("Push · Chest and triceps");

  const selectedDay = page.getByRole("region", { name: "Workouts for selected date" });
  await expect(selectedDay.getByText("Scheduled · Push · Chest and triceps")).toBeVisible();
  await selectedDay.getByRole("link", { name: /View workout for gym visit/ }).click();

  await page.getByRole("button", { name: "Edit workout" }).click();
  const workoutEditor = page.getByRole("dialog", { name: "Edit workout" });
  await expect(workoutEditor.getByLabel("Workout type")).toHaveValue("");
  await workoutEditor.getByLabel("Workout type").selectOption("double_muscle");
  await workoutEditor.getByLabel("Focus (optional)").fill("Back and biceps");
  await workoutEditor.getByRole("button", { name: "Save" }).click();
  await expect(workoutEditor).toBeHidden();
  await expect(page.getByText("Double muscle · Back and biceps", { exact: true })).toBeVisible();

  await page.getByRole("link", { name: "Back to Fitness" }).click();
  await expect(friday).toContainText("Push · Chest and triceps");
  await expect(selectedDay.getByRole("link", { name: /Double muscle · Back and biceps/ })).toBeVisible();
  await friday.click();
  await scheduleEditor.getByLabel("Workout type").selectOption("rest_day");
  await expect(scheduleEditor.getByLabel("Focus (optional)")).toBeHidden();
  await scheduleEditor.getByRole("button", { name: "Save" }).click();
  await expect(friday).toContainText("Rest day");
  await expect(selectedDay.getByRole("link", { name: /Double muscle · Back and biceps/ })).toBeVisible();
});
