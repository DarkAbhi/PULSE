"use client";

import { useId, useState } from "react";
import { Wrench } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";

type SeedResult = { days: number; completions: number };

export default function DebugSettingsButton() {
  const titleId = useId();
  const [open, setOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [result, setResult] = useState<SeedResult | null>(null);
  const [error, setError] = useState("");

  function close() {
    if (!pending) setOpen(false);
  }

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
    <>
      <PulseButton variant="onHero" size="lg" shape="pill" type="button" className="shrink-0 min-[562px]:ms-auto"
        onClick={() => setOpen(true)}>
        <Wrench aria-hidden="true" />
        Debug settings
      </PulseButton>
      {open && (
        <PulseDialog labelledBy={titleId} onDismiss={close}>
          <PulseDialogHeader id={titleId} icon={<Wrench className="size-5" />} onClose={close} closeDisabled={pending}>
            Debug settings
          </PulseDialogHeader>
          <section className="mt-5 space-y-2" aria-label="Quest history">
            <h3 className="font-semibold">Quest history</h3>
            <p className="text-sm text-muted-foreground">
              Fill unused dates in the past year with sample workouts, meals, activity rings, steps, and sleep for your account.
              Quest completions and rewards come from those records.
            </p>
            {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
            {result && (
              <p role="status" className="text-sm">
                {result.days > 0
                  ? `Added ${result.days} ${result.days === 1 ? "day" : "days"} and ${result.completions} quest ${result.completions === 1 ? "completion" : "completions"}.`
                  : "There are no unused dates in the past year."}
                {" "}<a href="/dashboard" className="font-semibold underline">View dashboard</a>
              </p>
            )}
          </section>
          <PulseDialogActions>
            <PulseDialogAction type="button" variant="secondary" onClick={close} disabled={pending}>
              Close
            </PulseDialogAction>
            <PulseDialogAction type="button" onClick={seedHistory} disabled={pending}>
              {pending ? "Seeding…" : "Seed history"}
            </PulseDialogAction>
          </PulseDialogActions>
        </PulseDialog>
      )}
    </>
  );
}
