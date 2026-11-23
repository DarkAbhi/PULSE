export const workoutTypes = {
  single_muscle: "Single muscle",
  double_muscle: "Double muscle",
  push: "Push",
  pull: "Pull",
  legs: "Legs",
  upper_body: "Upper body",
  lower_body: "Lower body",
  full_body: "Full body",
  cardio: "Cardio",
  mobility: "Mobility",
  other: "Other",
} as const;

type WorkoutType = keyof typeof workoutTypes;

export type WorkoutClassification = {
  workout_type: WorkoutType | null;
  workout_focus: string | null;
};

export type WorkoutScheduleDay = {
  weekday: number;
  workout_type: WorkoutType | "rest_day";
  workout_focus: string | null;
};

export function workoutLabel({ workout_type, workout_focus }: {
  workout_type?: WorkoutType | "rest_day" | null;
  workout_focus?: string | null;
}) {
  if (workout_type === "rest_day") return "Rest day";
  if (!workout_type) return null;
  const label = workoutTypes[workout_type];
  return workout_focus ? `${label} · ${workout_focus}` : label;
}
