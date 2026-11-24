import { Info } from "lucide-react";
import Image from "next/image";
import LocalDate from "@/app/components/local-date";
import LogoutButton from "@/app/profile/logout-button";
import PulseCard from "@/app/components/design-system/pulse-card";
import { sectionClass, sectionHeadingClass } from "@/app/profile/profile-classes";

export default function AboutPulse() {
  const version = process.env.NEXT_PUBLIC_PULSE_VERSION;
  const releasedAt = process.env.NEXT_PUBLIC_PULSE_RELEASE_DATE;
  const builtAt = process.env.NEXT_PUBLIC_PULSE_BUILD_DATE;

  return (
    <section className={sectionClass} aria-labelledby="about-pulse-title">
      <header>
        <h2 id="about-pulse-title" className={sectionHeadingClass}><Info aria-hidden="true" />About</h2>
      </header>
      <PulseCard className="grid overflow-hidden lg:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
        <div className="flex items-center gap-5 bg-(image:--profile-about-gradient) p-6">
          <span className="grid size-[76px] shrink-0 place-items-center rounded-3xl bg-card shadow-card">
            <Image src="/pulse-logo.png" alt="" width={46} height={46} />
          </span>
          <div>
            <h3 className="text-[2rem] leading-none font-extrabold tracking-[-0.04em]">Pulse</h3>
            <p className="mt-1.5 font-semibold text-muted-foreground">Your Life in Rhythm</p>
          </div>
        </div>
        <div className="p-6">
          <dl className="divide-y divide-divider [&>div]:flex [&>div]:items-baseline [&>div]:justify-between [&>div]:gap-3 [&>div]:py-2.5 [&_dt]:font-semibold [&_dt]:text-muted-foreground [&_dd]:text-right [&_dd]:font-extrabold [&_dd]:wrap-anywhere [&_dd]:tabular-nums">
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
      </PulseCard>
    </section>
  );
}
