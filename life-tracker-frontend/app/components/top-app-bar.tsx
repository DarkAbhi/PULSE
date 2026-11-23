import { Suspense } from "react";
import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import PulseAppBar, { PulseAppBarNotificationLink } from "@/app/components/design-system/pulse-app-bar";
import NotificationDropdown from "@/app/components/notification-dropdown";
import type { AppNotification } from "@/app/components/notification-list";

async function AppBarNotificationMenu() {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/notifications?limit=5`, {
      signal: AbortSignal.timeout(5_000),
    });
    if (response.ok) {
      const notifications = ((await response.json()) as AppNotification[] | null) ?? [];
      return <NotificationDropdown initialNotifications={notifications} appBar />;
    }
  } catch {
    // Keep the notification center reachable if the preview cannot be loaded.
  }

  return <PulseAppBarNotificationLink />;
}

export function AppBarNotifications({ notifications }: { notifications?: AppNotification[] }) {
  if (notifications) {
    return <NotificationDropdown initialNotifications={notifications.slice(0, 5)} appBar />;
  }

  return (
    <Suspense fallback={<PulseAppBarNotificationLink />}>
      <AppBarNotificationMenu />
    </Suspense>
  );
}

export default function TopAppBar({ backHref, backLabel, onProfilePage, notifications }: {
  backHref: string;
  backLabel: string;
  onProfilePage?: boolean;
  notifications?: AppNotification[];
}) {
  return (
    <PulseAppBar
      back={{ href: backHref }}
      backLabel={backLabel}
      onProfilePage={onProfilePage}
      notifications={<AppBarNotifications notifications={notifications} />}
    />
  );
}
