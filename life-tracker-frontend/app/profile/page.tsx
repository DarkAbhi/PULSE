import "@/app/profile/profile-theme.css";

import { Suspense } from "react";
import { UserRound } from "lucide-react";
import { redirect } from "next/navigation";
import { frame } from "@/app/components/design-system/page-frame";
import PulseCard from "@/app/components/design-system/pulse-card";
import TopAppBar from "@/app/components/top-app-bar";
import { getSession } from "@/app/lib/session";
import { getUserProfile } from "@/app/lib/user-profile";
import AboutPulse from "@/app/profile/about-pulse";
import APIKeysSection from "@/app/profile/api-keys-section";
import ChangePasswordButton from "@/app/profile/change-password-button";
import CurrencySettings from "@/app/profile/currency-settings";
import DashboardViewSettings from "@/app/profile/dashboard-view-settings";
import ThemeSettings from "@/app/profile/theme-settings";
import TimeZoneSettings from "@/app/profile/time-zone-settings";
import { sectionClass, sectionHeadingClass } from "@/app/profile/profile-classes";

export const metadata = {
  title: "Profile | PULSE",
};

export default async function ProfilePage() {
  const [session, profile] = await Promise.all([getSession(), getUserProfile().catch(() => null)]);
  if (!session) redirect("/");
  const { username } = session;
  const name = profile?.name;

  return (
    <main id="main-content" tabIndex={-1} className={frame.page}>
      <div className={`${frame.content} grid gap-8 [&>*]:min-w-0`}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" onProfilePage />

        <section className="relative isolate flex items-center gap-[clamp(18px,3vw,32px)] overflow-hidden rounded-[40px] bg-(image:--profile-hero-gradient) p-[clamp(22px,3vw,36px)] text-white shadow-(--purple-hero-shadow) max-[561px]:flex-col max-[561px]:items-start" aria-label="Your profile">
          <div className="pointer-events-none absolute -top-[60px] -right-[60px] -z-10 aspect-square w-[260px] bg-(image:--profile-hero-glow)" aria-hidden="true" />
          <div className="grid aspect-square w-[clamp(84px,12vw,120px)] shrink-0 place-items-center rounded-full border-2 border-white/50 bg-[linear-gradient(135deg,rgb(255_255_255/30%),rgb(255_255_255/10%))] shadow-[0_0_0_8px_rgb(255_255_255/10%)]" aria-hidden="true">
            <UserRound className="size-[52%] stroke-[1.6]" />
          </div>
          <div className="min-w-0">
            <h1 className="text-[clamp(2.2rem,5vw,3.6rem)] leading-none font-extrabold tracking-[-0.045em] wrap-anywhere">{name || username}</h1>
            <span className="mt-2.5 inline-block rounded-full border-[1.5px] border-white/40 bg-white/20 px-3.5 py-[5px] font-bold wrap-anywhere">@{username}</span>
          </div>
        </section>

        <div className="grid gap-5 min-[1001px]:grid-cols-2">
          <section className={sectionClass} aria-labelledby="profile-account">
            <header>
              <h2 id="profile-account" className={sectionHeadingClass}><UserRound aria-hidden="true" />Account</h2>
            </header>
            <PulseCard className="flex-1 divide-y divide-divider shadow-card">
              <ChangePasswordButton />
              <TimeZoneSettings />
              <CurrencySettings />
            </PulseCard>
          </section>

          <ThemeSettings />
        </div>

        <DashboardViewSettings />

        <Suspense fallback={<PulseCard className="p-6 shadow-card" role="status">Loading API keys…</PulseCard>}>
          <APIKeysSection />
        </Suspense>
        <AboutPulse />
      </div>
    </main>
  );
}
