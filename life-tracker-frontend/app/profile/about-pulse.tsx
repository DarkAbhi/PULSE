import PulseCard from "@/app/components/design-system/pulse-card";
import LocalDate from "@/app/components/local-date";

export default function AboutPulse() {
  const releasedAt = process.env.NEXT_PUBLIC_PULSE_RELEASE_DATE;
  const builtAt = process.env.NEXT_PUBLIC_PULSE_BUILD_DATE;

  return (
    <PulseCard as="section" aria-labelledby="about-pulse-title" className="mt-8 w-full p-6 text-left sm:p-8">
      <h2 id="about-pulse-title" className="text-lg font-semibold">About Pulse</h2>
      <div className="mt-6 flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          <img src="/pulse-logo.png" alt="Pulse logo" width={72} height={72} className="h-18 w-18 object-contain" />
          <div>
            <p aria-label="Pulse" className="text-3xl font-bold tracking-[0.18em] text-action">PULSE</p>
            <p className="mt-2 text-sm text-muted-foreground">Your life, in rhythm.</p>
          </div>
        </div>
        <dl className="space-y-3 text-sm sm:min-w-72">
          <div className="flex items-center justify-between gap-6">
            <dt className="text-muted-foreground">Version</dt>
            <dd className="rounded-full bg-primary/10 px-3 py-1 font-semibold text-action">v{process.env.NEXT_PUBLIC_PULSE_VERSION}</dd>
          </div>
          {releasedAt && (
            <div className="flex justify-between gap-6">
              <dt className="text-muted-foreground">Released</dt>
              <dd><LocalDate dateString={releasedAt} /></dd>
            </div>
          )}
          {builtAt && (
            <div className="flex justify-between gap-6">
              <dt className="text-muted-foreground">Built</dt>
              <dd><LocalDate dateString={builtAt} /></dd>
            </div>
          )}
        </dl>
      </div>
    </PulseCard>
  );
}
