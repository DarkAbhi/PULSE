import { Info } from "lucide-react";
import LocalDate from "@/app/components/local-date";
import LogoutButton from "./logout-button";
import styles from "./profile.module.css";

export default function AboutPulse() {
  const version = process.env.NEXT_PUBLIC_PULSE_VERSION;
  const releasedAt = process.env.NEXT_PUBLIC_PULSE_RELEASE_DATE;
  const builtAt = process.env.NEXT_PUBLIC_PULSE_BUILD_DATE;

  return (
    <section className={styles.section} aria-labelledby="about-pulse-title">
      <header className={styles.sectionHeader}>
        <h2 id="about-pulse-title"><Info aria-hidden="true" />About</h2>
      </header>
      <div className={`${styles.card} ${styles.about}`}>
        <div className={styles.brand}>
          <span className={styles.logo}>
            <svg width="46" height="46" viewBox="0 0 34 34" fill="none" aria-hidden="true">
              <defs>
                <linearGradient id="profile-pulse-logo" x1="0%" y1="100%" x2="100%" y2="0%">
                  <stop offset="0" stopColor="#19d3f3" />
                  <stop offset="0.5" stopColor="#4c6bff" />
                  <stop offset="1" stopColor="#ff2fd0" />
                </linearGradient>
              </defs>
              <path d="M3 17h7l4-10 6 20 4-10h7" stroke="url(#profile-pulse-logo)" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
          <div>
            <h3>Pulse</h3>
            <p>Your Life in Rhythm</p>
          </div>
        </div>
        <div className={styles.build}>
          <dl>
            {version && <div className={styles.detail}><dt>Version</dt><dd>v{version}</dd></div>}
            {releasedAt && (
              <div className={styles.detail}>
                <dt>Released</dt>
                <dd><LocalDate dateString={releasedAt} /></dd>
              </div>
            )}
            {builtAt && (
              <div className={styles.detail}>
                <dt>Build date and time</dt>
                <dd><LocalDate dateString={builtAt} /></dd>
              </div>
            )}
          </dl>
          <div className={styles.signOut}><LogoutButton /></div>
        </div>
      </div>
    </section>
  );
}
