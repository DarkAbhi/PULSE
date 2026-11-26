import "server-only";
import { readJSON } from "@/lib/api/server-api";
import type { AppNotification } from "@/features/notifications/components/notification-list";

export function loadNotifications() {
  return readJSON<AppNotification[]>("/api/notifications");
}
