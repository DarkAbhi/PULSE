import PulseCard from "@/components/ui/pulse-card";
import SignInForm from "@/features/auth/components/sign-in-form";

export function SignInScreen() {
  return (
    <main id="main-content" tabIndex={-1} className="flex min-h-screen items-center justify-center bg-background px-6 py-12 text-foreground">
      <PulseCard as="section" padding="lg" className="w-full max-w-md">
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
