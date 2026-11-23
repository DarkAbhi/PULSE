import { apiFetch } from "@/app/lib/server-api";
import { redirect } from "next/navigation";
import { frame } from "@/app/components/design-system/page-frame";
import TopAppBar from "@/app/components/top-app-bar";
import { AppNotification } from "@/app/components/notification-list";
import NotificationsClient from "@/app/notifications/notifications-client";

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export default async function NotificationsPage() {
  // Validate session on the server
  const sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`);
  if (!sessionResponse.ok) {
    redirect("/");
  }

  // Load notifications on the server
  const response = await apiFetch(`${apiBaseURL}/api/notifications`);
  const notifications = response.ok ? (await response.json()) as AppNotification[] : undefined;

  return (
    <main className={`${frame.page} bg-background text-foreground`}>
      <div className={frame.content}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" notifications={notifications} />
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
