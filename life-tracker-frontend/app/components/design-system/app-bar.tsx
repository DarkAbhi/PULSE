"use client";

import { ArrowLeft, UserRound } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

export const appBarControl = "relative grid size-11 shrink-0 place-items-center rounded-full bg-white text-[#1a1740] shadow-[0_1px_2px_rgb(26_23_64/6%),0_14px_32px_-20px_rgb(76_107_255/40%)] hover:bg-[#f1f2fb] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring dark:bg-[#1a1636] dark:text-[#f4f1ff] dark:shadow-[0_1px_2px_rgb(0_0_0/30%),0_14px_32px_-22px_rgb(0_0_0/80%)] dark:hover:bg-[#231e45]";

export function AppBarBell({ hasUnread = false }: { hasUnread?: boolean }) {
  return (
    <>
      <svg className="size-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path d="M12 22a2.4 2.4 0 0 0 2.3-1.8H9.7A2.4 2.4 0 0 0 12 22Zm7-6.2V11a7 7 0 0 0-5.2-6.8V3.5a1.8 1.8 0 0 0-3.6 0v.7A7 7 0 0 0 5 11v4.8l-1.6 1.6V19h17.2v-1.6L19 15.8Z" />
      </svg>
      {hasUnread && (
        <span aria-hidden="true" className="absolute top-2.5 right-[11px] size-[9px] rounded-full bg-[#ff2fd0] ring-[3px] ring-white dark:ring-[#1a1636]" />
      )}
    </>
  );
}

export function AppBarNotificationLink() {
  return (
    <Link href="/notifications" aria-label="Notifications" className={appBarControl}>
      <AppBarBell />
    </Link>
  );
}

type BackDestination = { href: string } | { onClick: () => void; disabled?: boolean };

export default function AppBar({ back, backLabel, notifications, onProfilePage = false }: {
  back: BackDestination;
  backLabel: string;
  notifications: ReactNode;
  onProfilePage?: boolean;
}) {
  const arrow = <ArrowLeft className="size-5" aria-hidden="true" />;

  return (
    <nav aria-label="App navigation" className="flex items-center justify-between gap-3 text-[#1a1740] dark:text-[#f4f1ff]">
      {"href" in back ? (
        <Link href={back.href} aria-label={backLabel} className={appBarControl}>{arrow}</Link>
      ) : (
        <button type="button" onClick={back.onClick} disabled={back.disabled} aria-label={backLabel} className={`${appBarControl} cursor-pointer disabled:cursor-default disabled:opacity-50`}>
          {arrow}
        </button>
      )}
      <div className="flex shrink-0 items-center gap-2">
        <Link href="/profile" aria-label="Profile" aria-current={onProfilePage ? "page" : undefined} className={appBarControl}>
          <UserRound className="size-5" aria-hidden="true" />
        </Link>
        {notifications}
      </div>
    </nav>
  );
}
