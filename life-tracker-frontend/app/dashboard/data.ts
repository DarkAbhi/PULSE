import "server-only";
import { getUserProfile } from "@/app/lib/user-profile";
import type { AppNotification } from "@/app/components/notification-list";
import type { HorizonSummary } from "@/app/financial-horizon/horizon-types";
import { apiFetch, apiBaseURL } from "@/app/lib/server-api";
import type { RecentActivityEntry } from "@/app/dashboard/recent-activity";
import type { FitnessToday } from "@/app/dashboard/fitness-at-a-glance";
import type { QuestActivity, QuestOverview } from "@/app/quests/types";

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
