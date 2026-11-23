import { UserRound } from "lucide-react";
import { redirect } from "next/navigation";
import frame from "@/app/components/design-system/page-frame.module.css";
import TopAppBar from "@/app/components/top-app-bar";
import { apiFetch } from "@/app/lib/server-api";
import AboutPulse from "./about-pulse";
import APIKeys from "./api-keys";
import ChangePasswordButton from "./change-password-button";
import CurrencySettings from "./currency-settings";
import ThemeSettings from "./theme-settings";
import TimeZoneSettings from "./time-zone-settings";
import { cardClass, profilePalette, sectionClass, sectionHeadingClass } from "./profile-classes";

export const metadata = {
  title: "Profile | PULSE",
};

const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";

export default async function ProfilePage() {
  const sessionResponse = await apiFetch(`${apiBaseURL}/api/auth/session`);
  if (!sessionResponse.ok) redirect("/");
  const { username } = (await sessionResponse.json()) as { username: string };

  const profileResponse = await apiFetch(`${apiBaseURL}/api/profile`);
  if (!profileResponse.ok) redirect("/");
  const { name } = (await profileResponse.json()) as { name?: string };

  return (
    <main className={frame.page}>
      <div className={`${frame.content} ${profilePalette} grid gap-[30px] text-(--profile-ink) [&>*]:min-w-0`}>
        <TopAppBar backHref="/dashboard" backLabel="Back to Dashboard" onProfilePage />

        <section className="relative isolate flex items-center gap-[clamp(18px,3vw,32px)] overflow-hidden rounded-[40px] bg-[linear-gradient(135deg,#2b1b8f,#5b2ce0_55%,#b22bd8)] p-[clamp(22px,3vw,36px)] text-white shadow-[0_22px_44px_-26px_rgb(91_44_224/90%)] max-[561px]:flex-col max-[561px]:items-start" aria-label="Your profile">
          <div className="pointer-events-none absolute -top-[60px] -right-[60px] -z-10 aspect-square w-[260px] bg-[radial-gradient(circle,rgb(25_211_243/50%),transparent_70%)]" aria-hidden="true" />
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
            <div className={`${cardClass} flex-1 divide-y divide-(--profile-line)`}>
              <ChangePasswordButton />
              <TimeZoneSettings />
              <CurrencySettings />
            </div>
          </section>

          <ThemeSettings />
        </div>

        <APIKeys />
        <AboutPulse />
      </div>
    </main>
  );
}
