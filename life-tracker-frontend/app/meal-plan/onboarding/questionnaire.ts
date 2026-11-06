import type { MealKind, MealPreferences, OnboardingState } from "./types";

export const goals = [
  ["feel_satisfied", "Feel full and satisfied"],
  ["food_calm", "Find more calm around food"],
  ["nourish_without_dieting", "Nourish myself without dieting"],
] as const;
export const barriers = [
  ["hungry_again", "I'm hungry again soon after eating"],
  ["too_tired", "I'm too tired to figure out meals"],
  ["unsure_what_to_add", "I don't know what to add"],
  ["food_advice_overwhelming", "Food advice feels overwhelming"],
] as const;
export const diets = [["no_preference", "No preference"], ["vegetarian", "Vegetarian"], ["vegan", "Vegan"], ["pescatarian", "Pescatarian"]] as const;
export const avoidances = [
  ["shellfish", "Shellfish", "🦐"], ["fish", "Fish", "🐟"], ["peanuts", "Peanuts", "🥜"], ["tree_nuts", "Tree nuts", "🌰"],
  ["dairy", "Dairy", "🥛"], ["eggs", "Eggs", "🥚"], ["gluten", "Gluten", "🌾"], ["soy", "Soy", "🫘"],
  ["sesame", "Sesame", "🌱"], ["pork", "Pork", "🐖"], ["beef", "Beef", "🐄"], ["poultry", "Poultry", "🍗"],
  ["mushrooms", "Mushrooms", "🍄"], ["onion_garlic", "Onion & garlic", "🧅"], ["spicy_food", "Spicy food", "🌶️"], ["alcohol", "Alcohol", "🍷"],
] as const;
export const mealKinds: { kind: MealKind; label: string; defaultTime: string }[] = [
  { kind: "breakfast", label: "Breakfast", defaultTime: "09:00" },
  { kind: "lunch", label: "Lunch", defaultTime: "12:30" },
  { kind: "dinner", label: "Dinner", defaultTime: "19:30" },
];
export const questions = [
  { title: "What's your goal?", description: "What would feel most helpful right now? Choose one." },
  { title: "What gets in the way?", description: "Pick any that sound familiar. You can leave this blank." },
  { title: "How do you like to eat?", description: "Choose what fits. There's no right answer." },
  { title: "Anything to avoid?", description: "Select any allergies or foods you dislike. You can leave this blank." },
  { title: "Where would a little help go furthest?", description: "Choose the meals you'd like help planning. Pick at least one." },
  { title: "When do you usually eat these meals?", description: "Set a usual time for each meal. You can change these anytime." },
];
export function initialPreferences(saved?: MealPreferences | null): MealPreferences {
  return saved ? structuredClone(saved) : { goal: "", barriers: [], dietary_pattern: "", food_avoidances: [], meals: mealKinds.map(m => ({ kind: m.kind, usual_time: m.defaultTime })) };
}
export function toggleChoice(values: string[], value: string) { return values.includes(value) ? values.filter(v => v !== value) : [...values, value]; }
export function stepError(step: number, p: MealPreferences): string {
  if (step === 0 && !goals.some(([code]) => code === p.goal)) return "Choose a goal to continue.";
  if (step === 2 && !diets.some(([code]) => code === p.dietary_pattern)) return "Choose how you like to eat.";
  if (step === 4 && p.meals.length === 0) return "Choose at least one meal to continue.";
  if (step === 5 && p.meals.some(m => !/^([01]\d|2[0-3]):[0-5]\d$/.test(m.usual_time))) return "Enter a valid time for every selected meal.";
  return "";
}
export function firstInvalidStep(p: MealPreferences) { return questions.findIndex((_, i) => Boolean(stepError(i, p))); }
export function shouldShowOnboarding(state: OnboardingState) { return state.status === "pending"; }
export function choiceLabel(options: readonly (readonly string[])[], code: string) { return options.find(option => option[0] === code)?.[1] ?? code; }

// Only built-in slots have these fixed names. A custom slot named "Lunch" is
// independent and must never inherit the system Lunch preference.
export function standardMealKind(slot: { is_default: boolean; name: string }): MealKind | undefined {
  if (!slot.is_default) return undefined;
  return ({ Breakfast: "breakfast", Lunch: "lunch", Dinner: "dinner" } as const)[slot.name as "Breakfast" | "Lunch" | "Dinner"];
}
