import { ArrowLeft, UserRound } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";

export const appBarControl = "relative grid size-11 shrink-0 place-items-center rounded-full bg-card text-foreground shadow-card hover:bg-muted";

export function PulseAppBarBell({ hasUnread = false }: { hasUnread?: boolean }) {
  return (
    <>
      <svg className="size-5" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path d="M12 22a2.4 2.4 0 0 0 2.3-1.8H9.7A2.4 2.4 0 0 0 12 22Zm7-6.2V11a7 7 0 0 0-5.2-6.8V3.5a1.8 1.8 0 0 0-3.6 0v.7A7 7 0 0 0 5 11v4.8l-1.6 1.6V19h17.2v-1.6L19 15.8Z" />
      </svg>
      {hasUnread && (
        <span aria-hidden="true" className="absolute top-2.5 right-2.5 size-2.5 rounded-full bg-brand-pink ring-3 ring-card" />
      )}
    </>
  );
}

export function PulseAppBarNotificationLink() {
  return (
    <Link href="/notifications" aria-label="Notifications" className={appBarControl}>
      <PulseAppBarBell />
    </Link>
  );
}

type BackDestination = { href: string } | { onClick: () => void; disabled?: boolean };

export default function PulseAppBar({ back, backLabel, notifications, onProfilePage = false, profileControl }: {
  back: BackDestination;
  backLabel: string;
  notifications: ReactNode;
  onProfilePage?: boolean;
  profileControl?: ReactNode;
}) {
  const arrow = <ArrowLeft className="size-5" aria-hidden="true" />;

  return (
    <nav aria-label="App navigation" className="flex items-center justify-between gap-3 text-foreground">
      {"href" in back ? (
        <Link href={back.href} aria-label={backLabel} className={appBarControl}>{arrow}</Link>
      ) : (
        <button type="button" onClick={back.onClick} disabled={back.disabled} aria-label={backLabel} className={`${appBarControl} cursor-pointer disabled:cursor-default disabled:opacity-50`}>
          {arrow}
        </button>
      )}
      <div className="flex shrink-0 items-center gap-2">
        {profileControl ?? (
          <Link href="/profile" aria-label="Profile" aria-current={onProfilePage ? "page" : undefined} className={appBarControl}>
            <UserRound className="size-5" aria-hidden="true" />
          </Link>
        )}
        {notifications}
      </div>
    </nav>
  );
}
