"use client";

import Button from "./components/design-system/button";

import { SubmitEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";

const apiBaseURL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export default function Home() {
  const router = useRouter();
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCheckingSession, setIsCheckingSession] = useState(true);

  useEffect(() => {
    async function redirectIfSignedIn() {
      try {
        const response = await fetch(`${apiBaseURL}/api/auth/session`, {
          credentials: "include",
        });
        if (response.ok) {
          router.replace("/dashboard");
          return;
        }
      } catch {
        // Leave the login form available when the API cannot be reached.
      }
      setIsCheckingSession(false);
    }

    void redirectIfSignedIn();
  }, [router]);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    const formData = new FormData(event.currentTarget);
    try {
      const response = await fetch(`${apiBaseURL}/api/auth/login`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: formData.get("username"),
          password: formData.get("password"),
        }),
      });
      const body = (await response.json()) as { error?: string };
      if (!response.ok) {
        setError(body.error ?? "Unable to sign in. Please try again.");
        return;
      }
      router.replace("/dashboard");
    } catch {
      setError("Unable to reach the server. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isCheckingSession) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-background text-sm text-muted-foreground">
        Checking your session…
      </main>
    );
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-6 py-12 text-foreground">
      <section className="w-full max-w-md rounded-2xl bg-card border border-border p-8 shadow-xl sm:p-10">
        <div className="mb-8">
          <p className="mb-2 text-sm font-semibold tracking-wide text-primary">
            LIFE TRACKER
          </p>
          <h1 className="text-3xl font-bold tracking-tight">Welcome back</h1>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            Sign in to continue to your dashboard.
          </p>
        </div>

        <form className="space-y-5" onSubmit={handleSubmit}>
          <div>
            <label
              className="mb-2 block text-sm font-medium text-muted-foreground"
              htmlFor="username"
            >
              Username
            </label>
            <input
              autoComplete="username"
              className="w-full rounded-lg border border-border bg-background px-4 py-3 text-base outline-none transition placeholder:text-muted-foreground focus:border-primary focus:ring-4 focus:ring-primary/15"
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
              className="w-full rounded-lg border border-border bg-background px-4 py-3 text-base outline-none transition placeholder:text-muted-foreground focus:border-primary focus:ring-4 focus:ring-primary/15"
              id="password"
              name="password"
              placeholder="Enter your password"
              required
              type="password"
            />
          </div>

          <Button variant="primary" size="lg"
            className="w-full"
            disabled={isSubmitting}
            type="submit"
          >
            {isSubmitting ? "Signing in…" : "Continue"}
          </Button>
          {error && <p className="text-center text-sm text-destructive" role="alert">{error}</p>}
        </form>
      </section>
    </main>
  );
}
