"use client";

import { useTimeZone } from "@/app/components/time-zone-provider";
import { useEffect, useState, type ReactNode } from "react";

const heroActionsClass =
  "flex shrink-0 gap-1 [&>div>button]:rounded-full! [&>div>button]:bg-transparent! [&>div>button]:text-inherit! " +
  "[&>div>button_svg]:text-inherit! [&>div>button:hover]:bg-white/15! max-[600px]:[&_[role=dialog]]:fixed " +
  "max-[600px]:[&_[role=dialog]]:top-22 max-[600px]:[&_[role=dialog]]:right-4 max-[600px]:[&_[role=dialog]]:left-4 " +
  "max-[600px]:[&_[role=dialog]]:w-auto max-[600px]:[&_[role=dialog]]:max-h-[calc(100dvh_-_112px)] " +
  "max-[600px]:[&_[role=dialog]]:overflow-y-auto";

const orbFillClass =
  "[background:radial-gradient(circle_at_30%_28%,#fff_0,#ffffffd9_12%,#ffffff00_34%),radial-gradient(circle_at_40%_40%,#19d3f3,#4c6bff_45%,#ff2fd0_85%)] " +
  "shadow-[inset_-20px_-30px_60px_#ffffff59,0_40px_80px_-30px_#4c6bff99] motion-safe:animate-pulse-breathe " +
  "dark:[background:radial-gradient(circle_at_35%_30%,#daf5ff,#64ccf3_20%,#446bff_40%,#ff2fd0_78%)] dark:shadow-none";

const orbClass =
  "absolute w-[min(64%,280px)] aspect-square right-[-22%] top-[-4%] rounded-full origin-center min-[600px]:w-[66%] " +
  "min-[600px]:max-w-none min-[600px]:right-[-16%] min-[600px]:top-[50%] min-[600px]:mt-[-33%]";

const heroArtClass =
  "absolute inset-0 -z-1 overflow-hidden rounded-[inherit] [background:white] shadow-[0_18px_40px_-24px_#4c6bff50] " +
  "before:absolute before:inset-0 " +
  "before:[background:radial-gradient(ellipse_at_85%_85%,#ff2fd059,transparent_70%),radial-gradient(ellipse_at_100%_45%,#19d3f366,transparent_70%),radial-gradient(ellipse_at_55%_100%,#4c6bff4d,transparent_70%)] " +
  "motion-safe:before:animate-pulse-sway dark:[background:linear-gradient(150deg,#2b1b8f_25%,#5b2ce0_65%,#b22bd8)] " +
  "dark:shadow-none dark:before:hidden";

const heroClass =
  "relative isolate flex min-h-90 min-w-0 flex-col justify-between gap-16 rounded-[32px] p-6 text-[#1a1740] " +
  "min-[600px]:min-h-100 min-[600px]:p-8 dark:text-white";

export function PulseOrb() {
  return (
    <div aria-hidden="true">
      {["", "[animation-delay:2s]", "[animation-delay:4s]"].map((delay) => (
        <div key={delay} className={`${orbClass} border-2 border-[#4c6bff73] animate-pulse-ripple dark:border-[#ffffff70] motion-reduce:hidden ${delay}`} />
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
          <p className="text-[24px] leading-[1.25] font-bold tabular-nums min-[600px]:text-[28px]">{now ? now.toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit", hour12: true, timeZone }).toUpperCase() : "—:—"}</p>
          <p className="mt-1 text-[14px] text-[#5e5a7d] max-w-[24ch] dark:text-[#ffffffe0] min-[600px]:text-[16px] min-[600px]:max-w-none">{now ? now.toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "short", year: "numeric", timeZone }) : "Your day at a glance"}</p>
        </div>
        <div className={heroActionsClass}>{children}</div>
      </div>
      <div className="min-[600px]:max-w-[68%]">
        <p className="dark:text-[#ffffffe0] text-[#5e5a7d] text-[20px]">Good to see you,</p>
        <h1 className="mt-3 mb-7 wrap-anywhere text-[clamp(48px,6vw,72px)] leading-[1.05] font-bold tracking-[-.035em]">{displayName || username}</h1>
        <p className="text-[15px] max-w-[36ch]">A little perspective on your everyday.</p>
      </div>
    </section>
  );
}
