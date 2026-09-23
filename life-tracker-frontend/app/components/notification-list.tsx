"use client";

import Button from "./design-system/button";

import { useRef, useState } from "react";
import LocalDate from "./local-date";

export type AppNotification = {
  id: number;
  source: string;
  title: string;
  body: string | null;
  target_path: string | null;
  priority: number;
  created_at: string;
};

type NotificationListProps = {
  notifications: AppNotification[];
  onDismiss: (notificationID: number) => Promise<void>;
  onMarkGymVisited?: (notificationID: number) => Promise<void>;
};

export function NotificationList({ notifications, onDismiss, onMarkGymVisited }: NotificationListProps) {
  return (
    <div className="space-y-3">
      {notifications.map((notification) => (
        <NotificationCard key={notification.id} notification={notification} onDismiss={onDismiss} onMarkGymVisited={onMarkGymVisited} />
      ))}
    </div>
  );
}

function NotificationCard({ notification, onDismiss, onMarkGymVisited }: { notification: AppNotification; onDismiss: (notificationID: number) => Promise<void>; onMarkGymVisited?: (notificationID: number) => Promise<void> }) {
  const startX = useRef<number | null>(null);
  const [dragOffset, setDragOffset] = useState(0);
  const [isDismissing, setIsDismissing] = useState(false);
  const [isMarkingGymVisited, setIsMarkingGymVisited] = useState(false);
  const isGymReminder = notification.source === "Gym reminder";

  async function dismiss() {
    setIsDismissing(true);
    try {
      await onDismiss(notification.id);
    } finally {
      setIsDismissing(false);
      setDragOffset(0);
    }
  }

  async function markGymVisited() {
    if (!onMarkGymVisited) return;
    setIsMarkingGymVisited(true);
    try {
      await onMarkGymVisited(notification.id);
    } finally {
      setIsMarkingGymVisited(false);
    }
  }

  return (
    <div className="relative overflow-hidden rounded-2xl bg-destructive">
      <div className="absolute inset-y-0 right-0 flex w-28 items-center justify-center text-sm font-semibold text-destructive-foreground">
        Dismiss
      </div>
      <article
        className="relative rounded-2xl border border-border bg-card p-5 shadow-sm transition-transform"
        onPointerDown={(event) => {
          startX.current = event.clientX;
          event.currentTarget.setPointerCapture(event.pointerId);
        }}
        onPointerMove={(event) => {
          if (startX.current === null) return;
          setDragOffset(Math.max(-140, Math.min(0, event.clientX - startX.current)));
        }}
        onPointerUp={() => {
          if (dragOffset <= -90) {
            void dismiss();
          } else {
            setDragOffset(0);
          }
          startX.current = null;
        }}
        style={{ transform: `translateX(${dragOffset}px)` }}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.14em] text-primary">{notification.source}</p>
            <h3 className="mt-1 font-semibold text-foreground">{notification.title}</h3>
          </div>
          <Button variant="tertiary" size="sm"
            aria-label={`Dismiss ${notification.title}`}
            disabled={isDismissing}
            onClick={(event) => {
               event.stopPropagation();
               void dismiss();
            }}
            onPointerDown={(event) => event.stopPropagation()}
            type="button"
            className="shrink-0"
          >
            Dismiss
          </Button>
        </div>
        {notification.body && <p className="mt-2 text-sm leading-6 text-muted-foreground">{notification.body}</p>}
        {isGymReminder && onMarkGymVisited && (
          <Button variant="primary" size="md"
            className="mt-4"
            disabled={isMarkingGymVisited || isDismissing}
            onClick={(event) => {
              event.stopPropagation();
              void markGymVisited();
            }}
            onPointerDown={(event) => event.stopPropagation()}
            type="button"
          >
            {isMarkingGymVisited ? "Saving your visit…" : "I visited the gym"}
          </Button>
        )}
        <p className="mt-3 text-xs text-muted-foreground/80">
          <LocalDate
            dateString={notification.created_at}
            options={{
              day: "numeric",
              month: "short",
              hour: "numeric",
              minute: "2-digit",
            }}
          />{" "}
          · Swipe left to dismiss
        </p>
      </article>
    </div>
  );
}
