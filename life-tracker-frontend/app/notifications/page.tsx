import { apiBaseURL, apiFetch } from "@/app/lib/server-api";
import { redirect } from "next/navigation";
import { frame } from "@/app/components/design-system/page-frame";
import TopAppBar from "@/app/components/top-app-bar";
import { AppNotification } from "@/app/components/notification-list";
import NotificationsClient from "@/app/notifications/notifications-client";

export const metadata = { title: "Notifications | PULSE" };

export default async function NotificationsPage() {
  const response = await apiFetch(`${apiBaseURL}/api/notifications`, {
    signal: AbortSignal.timeout(10_000),
  }).catch(() => null);
  if (response?.status === 401) redirect("/");
  const notifications: AppNotification[] | null = response?.ok ? await response.json().catch(() => null) : null;

  return (
    <main id="main-content" tabIndex={-1} className={`${frame.page} bg-background text-foreground`}>
      <div className={frame.content}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" notifications={notifications ?? undefined} />
        {notifications ? (
          <NotificationsClient notifications={notifications} />
        ) : (
          <>
            <header className="mt-5">
              <p className="text-sm font-semibold tracking-[0.18em] text-action">
                NOTIFICATIONS
              </p>
              <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
                Notification center
              </h1>
            </header>
            <p
              className="mt-10 rounded-xl bg-destructive-soft p-4 text-sm text-destructive"
              role="alert"
            >
              We couldn&apos;t load your notifications. Please try again.
            </p>
          </>
        )}
      </div>
    </main>
  );
}
