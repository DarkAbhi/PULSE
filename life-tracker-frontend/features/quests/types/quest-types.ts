export type QuestSettings = {
  gym_workout: boolean;
  step_target: number | null;
  activity_rings: boolean;
  sleep_minutes: number | null;
  daily_meals: boolean;
};

export type Quest = {
  id: "gym_workout" | "daily_steps" | "activity_rings" | "sleep_goal" | "daily_meals";
  target: number;
  progress: number;
  xp: number;
  hearts: number;
  completed: boolean;
};

export type QuestDay = {
  date: string;
  status: "complete" | "pending" | "missed" | "rest" | "future" | "unavailable";
  completed: number;
  total: number;
};

export type QuestActivity = {
  today: string;
  days: { date: string; completed_ids: string[] }[];
};

export type QuestOverview = {
  today: string;
  next_reset_at: string;
  configured: boolean;
  settings: QuestSettings;
  settings_effective?: string;
  has_workout_days: boolean;
  week: QuestDay[];
  previous_week?: string;
  next_week?: string;
  today_quests: Quest[];
  progress: {
    lifetime_xp: number;
    hearts: number;
    level: number;
    today_xp: number;
    today_xp_target: number;
    today_complete: number;
    week_xp: number;
    week_hearts: number;
    week_active: number;
    streak: number;
    best_streak: number;
  };
};
