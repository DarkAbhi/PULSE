import "server-only";
import { readJSON } from "@/lib/api/server-api";
import type { QuestOverview } from "@/features/quests/types/quest-types";

/** Loads one quest week; an unknown or invalid week falls back to the current one. */
export async function loadQuestOverview(week?: string | string[]) {
  const requestWeek = typeof week === "string" && /^\d{4}-\d{2}-\d{2}$/.test(week) ? week : "";
  const path = "/api/quests/overview";
  let overview = await readJSON<QuestOverview>(requestWeek ? `${path}?${new URLSearchParams({ week: requestWeek })}` : path);
  if (overview.status === 400 && requestWeek) overview = await readJSON<QuestOverview>(path);
  return overview;
}
