import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import ThemeSettings from "./theme-settings";
import LogoutButton from "./logout-button";
import ChangePasswordButton from "./change-password-button";

export const metadata = {
  title: "Profile | Life Tracker",
};

const apiBaseURL =
  process.env.NEXT_PUBLIC_INTERNAL_API_BASE_URL ??
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080";

type ProfileResponse = {
  has_profile: boolean;
  name?: string;
};

export default async function ProfilePage() {
  // Forward cookies from incoming request to backend for auth/session validation
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  // Validate session on the server
  const sessionResponse = await fetch(`${apiBaseURL}/api/auth/session`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  if (!sessionResponse.ok) {
    redirect("/");
  }
  const session = (await sessionResponse.json()) as { username: string };

  // Fetch profile on the server
  const profileResponse = await fetch(`${apiBaseURL}/api/profile`, {
    headers: {
      Cookie: cookieHeader,
    },
  });
  if (!profileResponse.ok) {
    redirect("/");
  }
  const profile = (await profileResponse.json()) as ProfileResponse;

  const username = session.username;
  const displayName = profile.name ?? "";

  return (
    <main className="min-h-screen bg-background px-6 py-10 text-foreground sm:px-10 lg:px-16">
      <div className="mx-auto max-w-2xl">
        <Link className="flex items-center gap-1 text-sm font-semibold text-primary transition hover:opacity-80 w-fit" href="/dashboard">
          <ArrowLeft className="h-4 w-4" /> Dashboard
        </Link>

        <section className="mt-12 flex flex-col items-center text-center">
          {/* Circular Profile Picture Placeholder on top */}
          <div className="relative mb-6">
            <div className="h-32 w-32 overflow-hidden rounded-full border-4 border-card shadow-xl ring-2 ring-primary/20">
              <img
                alt="Profile picture placeholder"
                className="h-full w-full object-cover"
                src="/avatar-placeholder.jpg"
              />
            </div>
          </div>

          <p className="text-xs font-semibold tracking-[0.18em] text-primary uppercase">
            Personal Profile
          </p>
          <h1 className="mt-3 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
            {displayName || username}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            @{username}
          </p>

          <div className="mt-10 w-full rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8 text-left">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
              <h2 className="text-lg font-semibold text-foreground">Account Information</h2>
              <ChangePasswordButton />
            </div>
            <div className="space-y-4 text-sm">
              <div className="flex justify-between py-2 border-b border-border">
                <span className="text-muted-foreground">Display Name</span>
                <span className="font-medium text-foreground">{displayName || "Not set"}</span>
              </div>
              <div className="flex justify-between py-2">
                <span className="text-muted-foreground">Username</span>
                <span className="font-medium text-foreground">{username}</span>
              </div>
            </div>
          </div>

          <ThemeSettings />

          <LogoutButton />
        </section>
      </div>
    </main>
  );
}
