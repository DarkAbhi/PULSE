"use client";

import { useState, useTransition } from "react";
import { saveNameAction } from "./actions";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";

interface NamePromptDialogProps {
  initialDisplayName: string;
}

export default function NamePromptDialog({
  initialDisplayName,
}: NamePromptDialogProps) {
  const [displayName, setDisplayName] = useState(initialDisplayName);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleSave(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    startTransition(async () => {
      const res = await saveNameAction(displayName);
      if (!res.ok) {
        setError(res.error ?? "Unable to save your name.");
      }
    });
  }

  return (
    <PulseDialog labelledBy="name-dialog-title">
        <PulseDialogHeader id="name-dialog-title" description="We’ll use it to personalize your dashboard.">
          What is your name?
        </PulseDialogHeader>
        <form className="mt-6 space-y-4" onSubmit={handleSave}>
          <label
            className="block text-sm font-medium text-muted-foreground"
            htmlFor="display-name"
          >
            Your name
          </label>
          <input
            autoFocus
            className="w-full rounded-lg bg-secondary text-foreground px-4 py-3 transition"
            id="display-name"
            maxLength={120}
            onChange={(event) => setDisplayName(event.target.value)}
            required
            value={displayName}
          />
          {error && (
            <p className="text-sm text-destructive" role="alert">
              {error}
            </p>
          )}
          <PulseDialogActions>
            <PulseDialogAction disabled={isPending} type="submit">
              {isPending ? "Saving…" : "Continue"}
            </PulseDialogAction>
          </PulseDialogActions>
        </form>
    </PulseDialog>
  );
}
