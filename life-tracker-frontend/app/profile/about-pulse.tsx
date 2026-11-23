import { Info } from "lucide-react";
import LocalDate from "@/app/components/local-date";
import LogoutButton from "./logout-button";
import { cardClass, sectionClass, sectionHeadingClass } from "./profile-classes";

export default function AboutPulse() {
  const version = process.env.NEXT_PUBLIC_PULSE_VERSION;
  const releasedAt = process.env.NEXT_PUBLIC_PULSE_RELEASE_DATE;
  const builtAt = process.env.NEXT_PUBLIC_PULSE_BUILD_DATE;

  return (
    <section className={sectionClass} aria-labelledby="about-pulse-title">
      <header>
        <h2 id="about-pulse-title" className={sectionHeadingClass}><Info aria-hidden="true" />About</h2>
      </header>
      <div className={`${cardClass} grid overflow-hidden min-[1001px]:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]`}>
        <div className="flex items-center gap-5 bg-[linear-gradient(135deg,rgb(76_107_255/14%),rgb(255_47_208/12%))] p-6">
          <span className="grid size-[76px] shrink-0 place-items-center rounded-3xl bg-(--profile-surface) shadow-(--profile-shadow)">
            <svg width="46" height="46" viewBox="0 0 34 34" fill="none" aria-hidden="true">
              <defs>
                <linearGradient id="profile-pulse-logo" x1="0%" y1="100%" x2="100%" y2="0%">
                  <stop offset="0" stopColor="#19d3f3" />
                  <stop offset="0.5" stopColor="#4c6bff" />
                  <stop offset="1" stopColor="#ff2fd0" />
                </linearGradient>
              </defs>
              <path d="M3 17h7l4-10 6 20 4-10h7" stroke="url(#profile-pulse-logo)" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
          <div>
            <h3 className="text-[2rem] leading-none font-extrabold tracking-[-0.04em]">Pulse</h3>
            <p className="mt-1.5 font-semibold text-(--profile-muted)">Your Life in Rhythm</p>
          </div>
        </div>
        <div className="p-6">
          <dl className="divide-y divide-(--profile-line) [&>div]:flex [&>div]:items-baseline [&>div]:justify-between [&>div]:gap-3 [&>div]:py-2.5 [&_dt]:font-semibold [&_dt]:text-(--profile-muted) [&_dd]:text-right [&_dd]:font-extrabold [&_dd]:wrap-anywhere [&_dd]:tabular-nums">
            {version && <div><dt>Version</dt><dd>v{version}</dd></div>}
            {releasedAt && (
              <div>
                <dt>Released</dt>
                <dd><LocalDate dateString={releasedAt} /></dd>
              </div>
            )}
            {builtAt && (
              <div>
                <dt>Build date and time</dt>
                <dd><LocalDate dateString={builtAt} /></dd>
              </div>
            )}
          </dl>
          <div className="flex justify-end pt-3"><LogoutButton /></div>
        </div>
      </div>
    </section>
  );
}
