"use client";

import { useEffect, useState } from "react";

interface GreetingHeaderProps {
  username: string;
  displayName: string;
}

export default function GreetingHeader({
  username,
  displayName,
}: GreetingHeaderProps) {
  const [greeting, setGreeting] = useState("Welcome back");
  const [greetingNote, setGreetingNote] = useState(
    "A fresh start is waiting for you."
  );

  useEffect(() => {
    const hour = new Date().getHours();
    if (hour < 12) {
      setGreeting("Good morning");
      setGreetingNote("A gentle start to a wonderful day.");
    } else if (hour < 17) {
      setGreeting("Good afternoon");
      setGreetingNote("Take a moment to check in with your day.");
    } else {
      setGreeting("Good evening");
      setGreetingNote("A quiet moment to reflect on today.");
    }
  }, []);

  return (
    <div>
      <p className="text-sm font-semibold tracking-[0.18em] text-primary">
        PULSE
      </p>
      <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
        {greeting}, {displayName || username}.
      </h1>
      <p className="mt-3 text-base text-muted-foreground">{greetingNote}</p>
    </div>
  );
}
