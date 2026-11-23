"use client";

import PulseCard from "@/app/components/design-system/pulse-card";

import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import PulseButton from "@/app/components/design-system/pulse-button";

import { useState, useTransition } from "react";
import { AppNotification, NotificationList } from "@/app/components/notification-list";
import { dismissNotification, clearAllNotifications } from "@/app/notifications/actions";
import { Trash2 } from "lucide-react";

interface NotificationsClientProps {
  notifications: AppNotification[];
}

export default function NotificationsClient({ notifications }: NotificationsClientProps) {
  const [error, setError] = useState("");
  const [confirmClear, setConfirmClear] = useState(false);
  const [isPending, startTransition] = useTransition();

  async function handleDismiss(notificationID: number) {
    setError("");
    const result = await dismissNotification(notificationID);
    if (!result.ok) setError(result.error ?? "We couldn’t dismiss that notification. Please try again.");
  }

  function handleClearAll() {
    setError("");
    startTransition(async () => {
      const res = await clearAllNotifications();
      if (res.ok) setConfirmClear(false);
      else setError(res.error ?? "We couldn’t clear your notifications. Please try again.");
    });
  }

  return (
    <>
      <header className="mt-5 flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm font-semibold tracking-[0.18em] text-action">NOTIFICATIONS</p>
          <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">Notification center</h1>
          <p className="mt-3 text-base text-muted-foreground">Stay in the loop across every part of PULSE.</p>
        </div>
        <PulseButton variant="destructive" size="lg"
          className="shrink-0"
          disabled={isPending || notifications.length === 0}
          onClick={() => { setError(""); setConfirmClear(true); }}
          type="button"
        >
          <Trash2 className="h-4 w-4" aria-hidden="true" />
          <span>{isPending ? "Clearing…" : "Clear all"}</span>
        </PulseButton>
      </header>

      <section className="mt-10" aria-label="All notifications">
        {error && !confirmClear ? (
          <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive mb-4" role="alert">{error}</p>
        ) : null}

        {notifications.length === 0 ? (
          <PulseCard className="p-10 text-center">
            <p className="text-lg font-semibold text-foreground">You&apos;re all caught up.</p>
            <p className="mt-2 text-sm text-muted-foreground">New updates from your life spaces will appear here.</p>
          </PulseCard>
        ) : (
          <NotificationList
            notifications={notifications}
            onDismiss={handleDismiss}
          />
        )}
      </section>
      <PulseConfirmationDialog
        isOpen={confirmClear}
        onClose={() => { setConfirmClear(false); setError(""); }}
        onConfirm={handleClearAll}
        title="Clear all notifications?"
        description="This permanently dismisses every notification in your notification center."
        confirmText="Clear all"
        confirmLoadingText="Clearing…"
        isLoading={isPending}
        error={error}
      />
    </>
  );
}
