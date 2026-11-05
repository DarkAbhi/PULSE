import test from "node:test";
import assert from "node:assert/strict";
import { initialPreferences, firstInvalidStep, stepError, shouldShowOnboarding, toggleChoice, standardMealKind } from "./questionnaire.ts";

test("only explicit terminal server states suppress setup", () => {
  assert.equal(shouldShowOnboarding({ status: "pending" }), true);
  assert.equal(shouldShowOnboarding({ status: "skipped" }), false);
  assert.equal(shouldShowOnboarding({ status: "completed" }), false);
  const draft = initialPreferences(); draft.goal = "food_calm";
  assert.equal(initialPreferences().goal, ""); // a remount is a fresh journey
});
test("goal, diet and a meal are required; optional questions accept no choices", () => {
  const p = initialPreferences();
  assert.equal(firstInvalidStep(p), 0);
  p.goal = "feel_satisfied";
  assert.equal(firstInvalidStep(p), 2);
  p.dietary_pattern = "no_preference";
  assert.equal(firstInvalidStep(p), -1);
  assert.equal(stepError(1, p), ""); assert.equal(stepError(3, p), "");
  p.meals = [];
  assert.equal(firstInvalidStep(p), 4);
});
test("only selected meal times are validated and times use HH:MM", () => {
  const p = initialPreferences(); p.goal = "food_calm"; p.dietary_pattern = "vegan";
  p.meals = [{ kind: "lunch", usual_time: "24:00" }];
  assert.equal(firstInvalidStep(p), 5);
  for (const invalid of ["", "9:00", "09:00:00", "12:60"]) { p.meals[0].usual_time = invalid; assert.notEqual(stepError(5,p), ""); }
  for (const valid of ["00:00", "12:30", "23:59"]) { p.meals[0].usual_time = valid; assert.equal(firstInvalidStep(p), -1); }
});
test("editing clones saved answers and multi-select toggles preserve other choices", () => {
  const saved = initialPreferences(); const draft = initialPreferences(saved);
  draft.meals[0].usual_time = "10:30"; draft.barriers.push("too_tired");
  assert.equal(saved.meals[0].usual_time, "09:00"); assert.deepEqual(saved.barriers, []);
  assert.deepEqual(toggleChoice(["fish"], "soy"), ["fish", "soy"]);
  assert.deepEqual(toggleChoice(["fish", "soy"], "fish"), ["soy"]);
});

test("usual meal preferences apply only to the matching system slots", () => {
  assert.equal(standardMealKind({ is_default: true, name: "Lunch" }), "lunch");
  assert.equal(standardMealKind({ is_default: false, name: "Lunch" }), undefined);
  assert.equal(standardMealKind({ is_default: true, name: "Evening Snacks" }), undefined);
});
