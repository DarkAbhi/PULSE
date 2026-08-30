"use client";

import { useState, useTransition } from "react";
import { saveNameAction } from "./actions";

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
    <div
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/60 p-4 backdrop-blur-xs sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="name-dialog-title"
    >
      <section className="w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-2xl sm:p-8">
        <h2
          className="text-2xl font-bold tracking-tight text-foreground"
          id="name-dialog-title"
        >
          What is your name?
        </h2>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          We&apos;ll use it to personalize your dashboard.
        </p>
        <form className="mt-6 space-y-4" onSubmit={handleSave}>
          <label
            className="block text-sm font-medium text-muted-foreground"
            htmlFor="display-name"
          >
            Your name
          </label>
          <input
            autoFocus
            className="w-full rounded-lg border border-border bg-background text-foreground px-4 py-3 outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/15"
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
          <button
            className="w-full rounded-lg bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:bg-muted"
            disabled={isPending}
            type="submit"
          >
            {isPending ? "Saving…" : "Continue"}
          </button>
        </form>
      </section>
    </div>
  );
}
