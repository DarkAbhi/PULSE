"use client";

import { useState } from "react";
import Link from "next/link";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseCard from "@/app/components/design-system/pulse-card";

type SeedResult = { days: number; completions: number };

export default function QuestHistorySeed() {
  const [pending, setPending] = useState(false);
  const [result, setResult] = useState<SeedResult | null>(null);
  const [error, setError] = useState("");

  async function seedHistory() {
    setPending(true);
    setError("");
    setResult(null);
    try {
      const response = await fetch("/api/debug/quest-history", { method: "POST" });
      if (response.status === 401) {
        setError("Your session expired. Please sign in again.");
        return;
      }
      if (!response.ok) throw new Error("Quest history seed failed");
      setResult(await response.json());
    } catch {
      setError("Couldn’t seed quest history. Try again.");
    } finally {
      setPending(false);
    }
  }

  return (
    <PulseCard as="section" aria-labelledby="quest-history" className="grid gap-4 p-5 shadow-card sm:p-7">
      <header className="space-y-2">
        <h2 id="quest-history" className="text-xl font-bold">Quest history</h2>
        <p className="max-w-prose text-sm leading-6 text-muted-foreground">
          Fill unused dates in the past year with sample workouts, meals, activity rings, steps, and sleep for your account.
          Quest completions and rewards come from those records.
        </p>
      </header>
      <PulseButton type="button" variant="gradient" size="lg" shape="pill" className="justify-self-start" onClick={seedHistory} disabled={pending}>
        {pending ? "Seeding…" : "Seed history"}
      </PulseButton>
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      <div role="status" className={pending || result ? "text-sm" : "sr-only"}>
        {pending && "Seeding quest history…"}
        {result && (
          <>
            {result.days > 0
              ? `Added ${result.days} ${result.days === 1 ? "day" : "days"} and ${result.completions} quest ${result.completions === 1 ? "completion" : "completions"}.`
              : "There are no unused dates in the past year."}
            {" "}<Link href="/dashboard" className="font-semibold underline">View dashboard</Link>
          </>
        )}
      </div>
    </PulseCard>
  );
}
