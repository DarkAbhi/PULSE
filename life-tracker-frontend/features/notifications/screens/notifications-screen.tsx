import { loadNotifications } from "@/features/notifications/server/notification-data";
import { redirect } from "next/navigation";
import { frame } from "@/components/layout/page-frame";
import TopAppBar from "@/features/notifications/components/top-app-bar";
import NotificationsClient from "@/features/notifications/components/notifications-client";

export async function NotificationsScreen() {
  const { status, data: notifications } = await loadNotifications();
  if (status === 401) redirect("/");

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
