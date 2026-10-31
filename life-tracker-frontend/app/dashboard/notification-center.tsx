"use client";

import PulseCard from "@/app/components/design-system/pulse-card";

import { useState } from "react";
import {
  AppNotification,
  NotificationList,
} from "@/app/components/notification-list";
import { dismissNotificationAction } from "./actions";

interface NotificationCenterProps {
  initialNotifications: AppNotification[];
}

export default function NotificationCenter({
  initialNotifications,
}: NotificationCenterProps) {
  const [notifications, setNotifications] =
    useState<AppNotification[]>(initialNotifications);
  const [error, setError] = useState("");

  async function handleDismiss(notificationID: number) {
    setError("");
    // Optimistic update
    const previousNotifications = notifications;
    setNotifications((current) =>
      current.filter((n) => n.id !== notificationID)
    );

    const res = await dismissNotificationAction(notificationID);
    if (!res.ok) {
      setError(res.error ?? "We couldn't dismiss that notification.");
      setNotifications(previousNotifications);
    }
  }

  if (notifications.length === 0) {
    return (
      <PulseCard className="p-7 text-center">
        <p className="font-semibold text-foreground">You&apos;re all caught up.</p>
        <p className="mt-1 text-sm text-muted-foreground">
          Updates from your spaces will appear here.
        </p>
      </PulseCard>
    );
  }

  return (
    <div className="space-y-4">
      {error && (
        <p className="rounded-xl bg-destructive/10 p-4 text-sm text-destructive" role="alert">
          {error}
        </p>
      )}
      <NotificationList
        notifications={notifications}
        onDismiss={handleDismiss}
      />
    </div>
  );
}
