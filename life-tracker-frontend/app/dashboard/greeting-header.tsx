"use client";

import { useTimeZone } from "@/app/components/time-zone-provider";

import { useEffect, useState, type ReactNode } from "react";

export function PulseOrb() {
  return (
    <div className="pulse-orb-scene" aria-hidden="true">
      {[0, 2, 4].map((delay) => (
        <div key={delay} className="pulse-orb pulse-orb-ring" style={{ animationDelay: `${delay}s` }} />
      ))}
      <div className="pulse-orb pulse-orb-fill" />
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
    <section className="pulse-hero" aria-label="Welcome">
      <div className="pulse-hero-art" aria-hidden="true"><PulseOrb /></div>
      <div className="pulse-hero-top">
        <div className="pulse-clock">
          <p className="pulse-clock-time">{now ? now.toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit", hour12: true, timeZone }).toUpperCase() : "—:—"}</p>
          <p className="pulse-clock-date">{now ? now.toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "short", year: "numeric", timeZone }) : "Your day at a glance"}</p>
        </div>
        <div className="pulse-hero-actions">{children}</div>
      </div>
      <div className="pulse-hero-greeting">
        <p className="pulse-greeting-eyebrow">Good to see you,</p>
        <h1>{displayName || username}</h1>
        <p className="pulse-greeting-note">A little perspective on your everyday.</p>
      </div>
    </section>
  );
}
