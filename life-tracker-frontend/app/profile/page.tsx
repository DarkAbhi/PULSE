import { UserRound } from "lucide-react";
import { redirect } from "next/navigation";
import BackLink from "@/app/components/design-system/back-link";
import { apiFetch } from "@/app/lib/server-api";
import AboutPulse from "./about-pulse";
import APIKeys from "./api-keys";
import ChangePasswordButton from "./change-password-button";
import CurrencySettings from "./currency-settings";
import ThemeSettings from "./theme-settings";
import TimeZoneSettings from "./time-zone-settings";
import styles from "./profile.module.css";

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
    <main className={styles.page}>
      <BackLink href="/dashboard" className={styles.back}>Dashboard</BackLink>

      <section className={styles.hero} aria-label="Your profile">
        <div className={styles.avatar} aria-hidden="true"><UserRound /></div>
        <div className="min-w-0">
          <h1>{name || username}</h1>
          <span className={styles.handle}>@{username}</span>
        </div>
      </section>

      <div className={styles.columns}>
        <section className={styles.section} aria-labelledby="profile-account">
          <header className={styles.sectionHeader}>
            <h2 id="profile-account"><UserRound aria-hidden="true" />Account</h2>
          </header>
          <div className={styles.card}>
            <ChangePasswordButton />
            <TimeZoneSettings />
            <CurrencySettings />
          </div>
        </section>

        <ThemeSettings />
      </div>

      <APIKeys />
      <AboutPulse />
    </main>
  );
}
