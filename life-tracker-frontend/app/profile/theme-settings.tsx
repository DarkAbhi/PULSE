"use client";

import { Check, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import styles from "./profile.module.css";

const options = [
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "system", label: "System" },
] as const;

export default function ThemeSettings() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <section className={styles.section} aria-labelledby="profile-theme">
      <header className={styles.sectionHeader}>
        <div>
          <h2 id="profile-theme"><Sun aria-hidden="true" />Theme</h2>
          <p id="profile-theme-help">Choose how Pulse looks on this device.</p>
        </div>
      </header>
      <div className={`${styles.card} ${styles.themes}`} role="radiogroup" aria-labelledby="profile-theme" aria-describedby="profile-theme-help">
        {options.map(({ value, label }) => (
          <label key={value} className={styles.themeOption}>
            <input type="radio" name="theme" value={value} checked={mounted && theme === value} onChange={() => setTheme(value)} />
            <span className={styles.themeBody}>
              <span className={`${styles.preview} ${styles[value]}`} aria-hidden="true">
                <span /><span /><span />
              </span>
              <span className={styles.themeLabel}>
                {label}
                <span className={styles.themeCheck} aria-hidden="true"><Check className="size-3.5 stroke-3" /></span>
              </span>
            </span>
          </label>
        ))}
      </div>
    </section>
  );
}
