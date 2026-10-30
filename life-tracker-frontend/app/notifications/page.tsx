import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { AppNotification } from "@/app/components/notification-list";
import NotificationsClient from "./notifications-client";
import { ArrowLeft } from "lucide-react";

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export default async function NotificationsPage() {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  // Validate session on the server
  const sessionResponse = await fetch(`${apiBaseURL}/api/auth/session`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  if (!sessionResponse.ok) {
    redirect("/");
  }

  // Load notifications on the server
  const response = await fetch(`${apiBaseURL}/api/notifications`, {
    headers: {
      Cookie: cookieHeader,
    },
  });

  if (!response.ok) {
    return (
      <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
        <div className="mx-auto max-w-6xl">
          <Link
            className="flex items-center gap-1 text-sm font-semibold text-action transition hover:opacity-80"
            href="/dashboard"
          >
            <ArrowLeft className="h-4 w-4" /> Dashboard
          </Link>
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
        </div>
      </main>
    );
  }

  const notifications = (await response.json()) as AppNotification[];

  return <NotificationsClient notifications={notifications} />;
}
