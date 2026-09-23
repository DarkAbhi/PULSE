"use client";

import Button from "../components/design-system/button";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Bell, ArrowRight, X } from "lucide-react";
import {
  AppNotification,
} from "../components/notification-list";
import {
  dismissNotificationAction,
  markGymReminderVisitedAction,
} from "./actions";
import LocalDate from "../components/local-date";

interface NotificationDropdownProps {
  initialNotifications: AppNotification[];
}

export default function NotificationDropdown({
  initialNotifications,
}: NotificationDropdownProps) {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] =
    useState<AppNotification[]>(initialNotifications);
  const [dismissingIds, setDismissingIds] = useState<Set<number>>(new Set());
  const [gymVisitIds, setGymVisitIds] = useState<Set<number>>(new Set());
  const [error, setError] = useState("");
  const panelRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  // Close on outside click
  useEffect(() => {
    if (!open) return;
    function handleClick(e: MouseEvent) {
      if (
        panelRef.current &&
        !panelRef.current.contains(e.target as Node) &&
        buttonRef.current &&
        !buttonRef.current.contains(e.target as Node)
      ) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [open]);

  // Close on Escape
  useEffect(() => {
    if (!open) return;
    function handleKey(e: KeyboardEvent) {
      if (e.key === "Escape") setOpen(false);
    }
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [open]);

  async function handleDismiss(id: number) {
    setError("");
    setDismissingIds((s) => new Set(s).add(id));
    const prev = notifications;
    setNotifications((cur) => cur.filter((n) => n.id !== id));
    const res = await dismissNotificationAction(id);
    if (!res.ok) {
      setError(res.error ?? "Couldn't dismiss that notification.");
      setNotifications(prev);
    }
    setDismissingIds((s) => {
      const next = new Set(s);
      next.delete(id);
      return next;
    });
  }

  async function handleMarkGymVisited(id: number) {
    setError("");
    setGymVisitIds((s) => new Set(s).add(id));
    const res = await markGymReminderVisitedAction(id);
    if (!res.ok) {
      setError(res.error ?? "Couldn't save that gym visit.");
    } else {
      setNotifications((cur) => cur.filter((n) => n.id !== id));
    }
    setGymVisitIds((s) => {
      const next = new Set(s);
      next.delete(id);
      return next;
    });
  }

  const unreadCount = notifications.length;

  return (
    <div className="relative">
      {/* Bell button */}
      <Button variant="iconSecondary" size="lg"
        ref={buttonRef}
        type="button"
        aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ""}`}
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className="relative"
      >
        <Bell className="h-5 w-5 text-foreground" />
        {unreadCount > 0 && (
          <span
            aria-hidden="true"
            className="absolute right-1.5 top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground"
          >
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </Button>

      {/* Dropdown panel */}
      {open && (
        <div
          ref={panelRef}
          role="dialog"
          aria-label="Notifications"
          className="absolute right-0 top-[calc(100%+10px)] z-50 w-[360px] max-w-[calc(100vw-2rem)] origin-top-right animate-dropdown rounded-2xl border border-border bg-card shadow-xl"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-border px-5 py-4">
            <div>
              <p className="font-semibold text-foreground">Notifications</p>
              {unreadCount > 0 && (
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {unreadCount} unread
                </p>
              )}
            </div>
            <Button variant="icon" size="sm"
              type="button"
              aria-label="Close notifications"
              onClick={() => setOpen(false)}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Body */}
          <div className="max-h-[380px] overflow-y-auto overscroll-contain px-4 py-3">
            {error && (
              <p className="mb-3 rounded-xl bg-destructive/10 px-4 py-3 text-xs text-destructive" role="alert">
                {error}
              </p>
            )}

            {notifications.length === 0 ? (
              <div className="py-8 text-center">
                <p className="font-semibold text-foreground">You&apos;re all caught up.</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  No new notifications right now.
                </p>
              </div>
            ) : (
              <ul className="space-y-2">
                {notifications.map((n) => {
                  const isGym = n.source === "Gym reminder";
                  const isBusy = dismissingIds.has(n.id) || gymVisitIds.has(n.id);
                  return (
                    <li
                      key={n.id}
                      className="rounded-xl border border-border bg-background p-4 transition hover:bg-muted/40"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <div className="min-w-0 flex-1">
                          <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-primary">
                            {n.source}
                          </p>
                          <p className="mt-0.5 text-sm font-semibold text-foreground leading-snug">
                            {n.title}
                          </p>
                          {n.body && (
                            <p className="mt-1 text-xs leading-5 text-muted-foreground line-clamp-2">
                              {n.body}
                            </p>
                          )}
                          <p className="mt-2 text-[10px] text-muted-foreground/70">
                            <LocalDate
                              dateString={n.created_at}
                              options={{
                                day: "numeric",
                                month: "short",
                                hour: "numeric",
                                minute: "2-digit",
                              }}
                            />
                          </p>
                        </div>
                        <Button variant="tertiary" size="sm"
                          type="button"
                          aria-label={`Dismiss ${n.title}`}
                          disabled={isBusy}
                          onClick={() => void handleDismiss(n.id)}
                          className="shrink-0"
                        >
                          {dismissingIds.has(n.id) ? "…" : "Dismiss"}
                        </Button>
                      </div>

                      {isGym && (
                        <Button variant="primary" size="sm"
                          type="button"
                          disabled={isBusy}
                          onClick={() => void handleMarkGymVisited(n.id)}
                          className="mt-3 w-full"
                        >
                          {gymVisitIds.has(n.id) ? "Saving…" : "I visited the gym"}
                        </Button>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>

          {/* Footer */}
          <div className="border-t border-border px-5 py-3">
            <Link
              href="/notifications"
              onClick={() => setOpen(false)}
              className="flex w-full items-center justify-center gap-1.5 rounded-xl bg-primary/10 px-4 py-2.5 text-sm font-semibold text-primary transition hover:bg-primary/20"
            >
              View all notifications
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
