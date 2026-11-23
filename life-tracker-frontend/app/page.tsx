import { redirect } from "next/navigation";
import PulseCard from "@/app/components/design-system/pulse-card";
import { getSession } from "@/app/lib/session";
import SignInForm from "@/app/login/sign-in-form";

export default async function LoginPage() {
  if (await getSession()) redirect("/dashboard");

  return (
    <main id="main-content" tabIndex={-1} className="flex min-h-screen items-center justify-center bg-background px-6 py-12 text-foreground">
      <PulseCard as="section" className="w-full max-w-md p-8 shadow-xl sm:p-10">
        <div className="mb-8">
          <p className="mb-2 text-sm font-semibold tracking-wide text-action">
            PULSE
          </p>
          <h1 className="text-3xl font-bold tracking-tight">Welcome back</h1>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            Sign in to continue to your dashboard.
          </p>
        </div>

        <SignInForm />
      </PulseCard>
    </main>
  );
}
