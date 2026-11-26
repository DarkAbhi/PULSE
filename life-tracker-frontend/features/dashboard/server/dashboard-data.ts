import "server-only";
import { getUserProfile } from "@/lib/user/user-profile";
import type { AppNotification } from "@/features/notifications";
import type { HorizonSummary } from "@/features/financial-horizon";
import { apiFetch, apiBaseURL } from "@/lib/api/server-api";
import type { RecentActivityEntry } from "@/features/dashboard/components/recent-activity";
import type { FitnessToday } from "@/features/dashboard/components/fitness-at-a-glance";
import type { QuestActivity, QuestOverview } from "@/features/quests";

async function readDashboardResource<T>(path: string): Promise<T | null> {
  try {
    const response = await apiFetch(`${apiBaseURL}${path}`, {
      signal: AbortSignal.timeout(10_000),
    });
    if (!response.ok) return null;
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

export function getDashboardData() {
  return {
    profile: getUserProfile().catch(() => null),
    workout: readDashboardResource<{ visited: boolean }>("/api/workout/today"),
    fitness: readDashboardResource<FitnessToday>("/api/fitness/today"),
    questSettings: readDashboardResource<Pick<QuestOverview, "settings" | "settings_effective">>("/api/quests/settings"),
    questActivity: readDashboardResource<QuestActivity>("/api/quests/activity"),
    recentActivity: readDashboardResource<RecentActivityEntry[]>("/api/activity/recent"),
    notifications: readDashboardResource<AppNotification[]>("/api/notifications?limit=5"),
    horizon: readDashboardResource<Pick<HorizonSummary, "currency" | "remaining_amount">>("/api/horizon"),
  };
}
