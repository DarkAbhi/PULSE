import type { ReactNode } from "react";
import DashboardClock from "@/features/dashboard/components/dashboard-clock";

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
  "compact:min-h-100 compact:p-8 dark:text-white dashboard-compact:min-h-44 dashboard-compact:gap-5 " +
  "dashboard-compact:p-4 dashboard-compact:sm:px-7 dashboard-compact:sm:py-5 dashboard-compact:rounded-card";

function PulseOrb() {
  return (
    <div aria-hidden="true">
      {["", "[animation-delay:2s]", "[animation-delay:4s]"].map((delay) => (
        <div key={delay} className={`${orbClass} border-2 border-brand-blue/45 animate-pulse-ripple dark:border-white/44 motion-reduce:hidden ${delay}`} />
      ))}
      <div className={`${orbClass} ${orbFillClass}`} />
    </div>
  );
}

interface GreetingHeaderProps {
  name: string;
  children?: ReactNode;
}

export default function GreetingHeader({ name, children }: GreetingHeaderProps) {
  return (
    <section className={heroClass} aria-label="Welcome">
      <div className={heroArtClass} aria-hidden="true"><PulseOrb /></div>
      <div className="flex justify-between items-start gap-2">
        <DashboardClock />
        <div className="flex shrink-0 gap-1">{children}</div>
      </div>
      <div className="compact:max-w-[68%] dashboard-compact:compact:max-w-[60%]">
        <p className="dark:text-white/88 text-muted-foreground text-xl dashboard-compact:text-base">Good to see you,</p>
        <div className="dashboard-compact:mt-1 dashboard-compact:lg:flex dashboard-compact:lg:items-center dashboard-compact:lg:gap-5">
          <h1 className="mt-3 mb-7 wrap-anywhere text-[clamp(48px,6vw,72px)] leading-[1.05] font-bold tracking-[-.035em] dashboard-compact:my-0 dashboard-compact:text-[clamp(2.25rem,4vw,3.5rem)]">{name}</h1>
          <p className="text-control max-w-[36ch] dashboard-compact:mt-1 dashboard-compact:lg:mt-0 dashboard-compact:lg:border-s dashboard-compact:lg:border-current/35 dashboard-compact:lg:ps-5">A little perspective on your everyday.</p>
        </div>
      </div>
    </section>
  );
}
