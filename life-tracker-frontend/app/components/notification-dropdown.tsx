"use client";

import PulseButton from "@/app/components/design-system/pulse-button";
import PulseIconButton from "@/app/components/design-system/pulse-icon-button";

import { useEffect, useId, useOptimistic, useRef, useState, useTransition } from "react";
import Link from "next/link";
import { Bell, ArrowRight, X } from "lucide-react";
import type { AppNotification } from "@/app/components/notification-list";
import { dismissNotification } from "@/app/notifications/actions";
import { PulseAppBarBell, appBarControl } from "@/app/components/design-system/pulse-app-bar";
import LocalDate from "@/app/components/local-date";

interface NotificationDropdownProps {
  initialNotifications: AppNotification[];
  appBar?: boolean;
}

export default function NotificationDropdown({
  initialNotifications,
  appBar = false,
}: NotificationDropdownProps) {
  const [open, setOpen] = useState(false);
  const [notifications, hideNotification] = useOptimistic(
    initialNotifications,
    (current, id: number) => current.filter((notification) => notification.id !== id),
  );
  const [, startTransition] = useTransition();
  const [error, setError] = useState("");
  const panelId = useId();
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

  function closeMenu() {
    setOpen(false);
    buttonRef.current?.focus();
  }

  useEffect(() => {
    if (open) panelRef.current?.focus();
  }, [open]);

  function handleDismiss(id: number) {
    setError("");
    startTransition(async () => {
      hideNotification(id);
      const result = await dismissNotification(id);
      if (!result.ok) {
        setError(result.error ?? "Couldn't dismiss that notification.");
      }
    });
  }

  const unreadCount = notifications.length;

  return (
    <div className="relative" onKeyDown={event => {
      if (open && event.key === "Escape") {
        event.preventDefault();
        closeMenu();
      }
    }}>
      {appBar ? (
        <button
          ref={buttonRef}
          type="button"
          aria-label={unreadCount ? "Notifications, unread updates" : "Notifications"}
          aria-haspopup="dialog"
          aria-expanded={open}
          aria-controls={open ? panelId : undefined}
          onClick={() => setOpen((value) => !value)}
          className={`${appBarControl} cursor-pointer`}
        >
          <PulseAppBarBell hasUnread={unreadCount > 0} />
        </button>
      ) : (
        <PulseIconButton
          variant="overlay"
          size="lg"
          ref={buttonRef}
          type="button"
          aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ""}`}
          aria-haspopup="dialog"
          aria-expanded={open}
          aria-controls={open ? panelId : undefined}
          onClick={() => setOpen((value) => !value)}
          className="relative"
        >
          <Bell className="size-5" aria-hidden="true" />
          {unreadCount > 0 && (
            <span
              aria-hidden="true"
              className="absolute right-1.5 top-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground"
            >
              {unreadCount > 9 ? "9+" : unreadCount}
            </span>
          )}
        </PulseIconButton>
      )}

      {/* Dropdown panel */}
      {open && (
        <div
          ref={panelRef}
          id={panelId}
          tabIndex={-1}
          role="dialog"
          aria-label="Notifications"
          className={`absolute right-0 top-full z-50 mt-2.5 w-90 max-w-[calc(100vw-3rem)] origin-top-right rounded-2xl bg-card shadow-xl motion-safe:animate-dropdown ${appBar ? "" : "max-compact:fixed max-compact:inset-x-4 max-compact:top-22 max-compact:mt-0 max-compact:max-h-[calc(100dvh-7rem)] max-compact:w-auto max-compact:max-w-none max-compact:overflow-y-auto"}`}
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-divider px-5 pt-5 pb-2">
            <div>
              <p className="font-semibold text-foreground">Notifications</p>
              {unreadCount > 0 && (
                <p className="mt-0.5 text-xs text-muted-foreground">
                  {unreadCount} unread
                </p>
              )}
            </div>
            <PulseIconButton size="sm"
              type="button"
              aria-label="Close notifications"
              onClick={closeMenu}
            >
              <X className="h-4 w-4" />
            </PulseIconButton>
          </div>

          {/* Body */}
          <div className="max-h-[380px] overflow-y-auto overscroll-contain px-3 py-2">
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
              <ul className="space-y-1">
                {notifications.map((n) => {
                  return (
                    <li
                      key={n.id}
                      className="rounded-xl p-3 transition hover:bg-secondary/60 focus-within:bg-secondary/60"
                    >
                      <div className="flex items-start justify-between gap-3">
                        <span aria-hidden="true" className="mt-1.5 size-2 shrink-0 rounded-full bg-action" />
                        <div className="min-w-0 flex-1">
                          <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-action">
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
                        <PulseButton variant="ghost" size="sm"
                          type="button"
                          aria-label={`Dismiss ${n.title}`}
                          onClick={() => handleDismiss(n.id)}
                          className="shrink-0"
                        >
                          Dismiss
                        </PulseButton>
                      </div>

                    </li>
                  );
                })}
              </ul>
            )}
          </div>

          {/* Footer */}
          <div className="border-t border-divider px-5 pt-3 pb-5">
            <Link
              href="/notifications"
              onClick={() => setOpen(false)}
              className="flex w-full items-center justify-center gap-1.5 rounded-xl bg-primary/10 px-4 py-2.5 text-sm font-semibold text-action transition hover:bg-primary/20"
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
