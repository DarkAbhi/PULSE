"use client";

import { useState, useTransition } from "react";
import { saveNameAction } from "./actions";
import Dialog, { DialogAction } from "../components/design-system/dialog";

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
    <Dialog labelledBy="name-dialog-title">
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
          <DialogAction
            className="mt-6 w-full"
            disabled={isPending}
            type="submit"
          >
            {isPending ? "Saving…" : "Continue"}
          </DialogAction>
        </form>
    </Dialog>
  );
}
