"use client";

import Link from "next/link";
import { useState, useTransition } from "react";
import { AppNotification, NotificationList } from "../components/notification-list";
import { dismissNotification, markGymVisited, clearAllNotifications } from "./actions";
import { ArrowLeft, Trash2 } from "lucide-react";

interface NotificationsClientProps {
  notifications: AppNotification[];
}

export default function NotificationsClient({ notifications }: NotificationsClientProps) {
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  async function handleDismiss(notificationID: number) {
    setError("");
    return new Promise<void>((resolve) => {
      startTransition(async () => {
        const res = await dismissNotification(notificationID);
        if (!res.ok) {
          setError(res.error ?? "We couldn't dismiss that notification. Please try again.");
        }
        resolve();
      });
    });
  }

  async function handleMarkGymVisited(notificationID: number) {
    setError("");
    return new Promise<void>((resolve) => {
      startTransition(async () => {
        const res = await markGymVisited(notificationID);
        if (!res.ok) {
          setError(res.error ?? "We couldn't save that gym visit. Please try again.");
        }
        resolve();
      });
    });
  }

  function handleClearAll() {
    setError("");
    startTransition(async () => {
      const res = await clearAllNotifications();
      if (!res.ok) {
        setError(res.error ?? "We couldn't clear your notifications. Please try again.");
      }
    });
  }

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-6xl">
        <Link className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80 w-fit" href="/dashboard">
          <ArrowLeft className="h-4 w-4" /> Dashboard
        </Link>
        <header className="mt-5 flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold tracking-[0.18em] text-primary">NOTIFICATIONS</p>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">Notification center</h1>
            <p className="mt-3 text-base text-muted-foreground">Stay in the loop across every part of your life tracker.</p>
          </div>
          <button
            className="shrink-0 flex items-center gap-1.5 rounded-lg border border-destructive px-4 py-3 text-sm font-semibold text-destructive transition hover:bg-destructive/10 disabled:cursor-not-allowed disabled:opacity-50"
            disabled={isPending || notifications.length === 0}
            onClick={handleClearAll}
            type="button"
          >
            <Trash2 className="h-4 w-4" />
            <span>{isPending ? "Clearing…" : "Clear all"}</span>
          </button>
        </header>

        <section className="mt-10" aria-label="All notifications">
          {error ? (
            <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive mb-4" role="alert">{error}</p>
          ) : null}
          
          {notifications.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-border bg-card p-10 text-center">
              <p className="text-lg font-semibold text-foreground">You&apos;re all caught up.</p>
              <p className="mt-2 text-sm text-muted-foreground">New updates from your life spaces will appear here.</p>
            </div>
          ) : (
            <NotificationList
              notifications={notifications}
              onDismiss={handleDismiss}
              onMarkGymVisited={handleMarkGymVisited}
            />
          )}
        </section>
      </div>
    </main>
  );
}
