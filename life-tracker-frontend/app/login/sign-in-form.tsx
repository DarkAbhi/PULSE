"use client";

import { useRouter } from "next/navigation";
import { useState, type SubmitEvent } from "react";
import PulseButton from "@/app/components/design-system/pulse-button";

export default function SignInForm() {
  const router = useRouter();
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSubmitting) return;

    setError("");
    setIsSubmitting(true);

    const formData = new FormData(event.currentTarget);
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: formData.get("username"),
          password: formData.get("password"),
        }),
      });

      if (!response.ok) {
        const body = await response.json().catch(() => null);
        setError(
          typeof body?.error === "string" && body.error
            ? body.error
            : "Unable to sign in. Please try again.",
        );
        setIsSubmitting(false);
        return;
      }

      router.replace("/dashboard");
      router.refresh();
    } catch {
      setError("Unable to reach the server. Please try again.");
      setIsSubmitting(false);
    }
  }

  return (
    <form method="post" aria-busy={isSubmitting} className="space-y-5" onSubmit={handleSubmit}>
      <div>
        <label
          className="mb-2 block text-sm font-medium text-muted-foreground"
          htmlFor="username"
        >
          Username
        </label>
        <input
          autoComplete="username"
          spellCheck={false}
          className="w-full rounded-lg bg-secondary px-4 py-3 text-base transition placeholder:text-muted-foreground"
          id="username"
          name="username"
          defaultValue="admin"
          placeholder="Enter your username"
          required
          type="text"
        />
      </div>

      <div>
        <label
          className="mb-2 block text-sm font-medium text-muted-foreground"
          htmlFor="password"
        >
          Password
        </label>
        <input
          autoComplete="current-password"
          className="w-full rounded-lg bg-secondary px-4 py-3 text-base transition placeholder:text-muted-foreground"
          id="password"
          name="password"
          placeholder="Enter your password"
          required
          type="password"
        />
      </div>

      <PulseButton
        variant="primary"
        size="lg"
        className="w-full"
        disabled={isSubmitting}
        type="submit"
      >
        {isSubmitting ? "Signing in…" : "Sign in"}
      </PulseButton>
      {error && (
        <p className="text-center text-sm text-destructive" role="alert">{error}</p>
      )}
    </form>
  );
}
