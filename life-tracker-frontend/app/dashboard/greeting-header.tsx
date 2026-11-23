"use client";

import { useTimeZone } from "@/app/components/time-zone-provider";
import { useEffect, useState, type ReactNode } from "react";

const orbFillClass =
  "bg-(image:--dashboard-orb-fill) shadow-(--dashboard-orb-shadow) motion-safe:animate-pulse-breathe";

const orbClass =
  "absolute w-[min(64%,280px)] aspect-square -right-[22%] -top-[4%] rounded-full origin-center compact:w-[66%] " +
  "compact:max-w-none compact:-right-[16%] compact:top-1/2 compact:-mt-[33%]";

const heroArtClass =
  "absolute inset-0 -z-1 overflow-hidden rounded-[inherit] bg-card shadow-card " +
  "before:absolute before:inset-0 before:bg-(image:--dashboard-hero-light) motion-safe:before:animate-pulse-sway " +
  "dark:bg-(image:--dashboard-hero-dark) dark:shadow-none dark:before:hidden";

const heroClass =
  "relative isolate flex min-h-90 min-w-0 flex-col justify-between gap-16 rounded-hero p-6 text-foreground " +
  "compact:min-h-100 compact:p-8 dark:text-white";

export function PulseOrb() {
  return (
    <div aria-hidden="true">
      {["", "[animation-delay:2s]", "[animation-delay:4s]"].map((delay) => (
        <div key={delay} className={`${orbClass} border-2 border-brand-blue/45 animate-pulse-ripple dark:border-white/44 motion-reduce:hidden ${delay}`} />
      ))}
      <div className={`${orbClass} ${orbFillClass}`} />
    </div>
  );
}

export default function GreetingHeader({ username, displayName, children }: {
  username: string;
  displayName: string;
  children?: ReactNode;
}) {
  const { timeZone } = useTimeZone();
  const [now, setNow] = useState<Date | null>(null);

  useEffect(() => {
    const update = () => setNow(new Date());
    update();
    const timer = window.setInterval(update, 1000);
    return () => window.clearInterval(timer);
  }, []);

  return (
    <section className={heroClass} aria-label="Welcome">
      <div className={heroArtClass} aria-hidden="true"><PulseOrb /></div>
      <div className="flex justify-between items-start gap-2">
        <div className="min-w-0">
          <p data-clock="time" className="text-2xl leading-[1.25] font-bold tabular-nums compact:text-[28px]">{now ? now.toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit", hour12: true, timeZone }).toUpperCase() : "—:—"}</p>
          <p data-clock="date" className="mt-1 text-sm text-muted-foreground max-w-[24ch] dark:text-white/88 compact:text-base compact:max-w-none">{now ? now.toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "short", year: "numeric", timeZone }) : "Your day at a glance"}</p>
        </div>
        <div className="flex shrink-0 gap-1">{children}</div>
      </div>
      <div className="compact:max-w-[68%]">
        <p className="dark:text-white/88 text-muted-foreground text-xl">Good to see you,</p>
        <h1 className="mt-3 mb-7 wrap-anywhere text-[clamp(48px,6vw,72px)] leading-[1.05] font-bold tracking-[-.035em]">{displayName || username}</h1>
        <p className="text-control max-w-[36ch]">A little perspective on your everyday.</p>
      </div>
    </section>
  );
}
