"use client";

import { Check, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import { PulseChoiceControl } from "@/components/ui/pulse-choice-group";
import PulseCard from "@/components/ui/pulse-card";
import { sectionClass, sectionDescriptionClass, sectionHeadingClass } from "@/features/profile/styles/profile-classes";

const options = [
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "system", label: "System" },
] as const;

const previews = {
  light: { surface: "bg-(--light-background)", heading: "bg-(--light-foreground)", card: "bg-(--light-card)", line: "bg-(--profile-preview-light-line)" },
  dark: { surface: "bg-(--dark-background)", heading: "bg-(--dark-foreground)", card: "bg-(--dark-card)", line: "bg-(--profile-preview-dark-line)" },
  system: {
    surface: "bg-[linear-gradient(115deg,var(--light-background)_50%,var(--dark-background)_50%)]",
    heading: "bg-[linear-gradient(90deg,var(--light-foreground)_50%,var(--dark-foreground)_50%)]",
    card: "bg-[linear-gradient(90deg,var(--light-card)_50%,var(--dark-card)_50%)]",
    line: "bg-[linear-gradient(90deg,var(--profile-preview-light-line)_50%,var(--profile-preview-dark-line)_50%)]",
  },
};

export default function ThemeSettings() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <section className={sectionClass} aria-labelledby="profile-theme">
      <header>
        <h2 id="profile-theme" className={sectionHeadingClass}><Sun aria-hidden="true" />Theme</h2>
        <p id="profile-theme-help" className={sectionDescriptionClass}>Choose how Pulse looks on this device.</p>
      </header>
      <PulseCard padding="md" className="grid flex-1 gap-3 sheet:grid-cols-3" role="radiogroup" aria-labelledby="profile-theme" aria-describedby="profile-theme-help">
        {options.map(({ value, label }) => (
          <PulseChoiceControl key={value} className="block min-w-0" type="radio" name="theme" value={value} checked={mounted && theme === value} onCheckedChange={() => setTheme(value)}>
            <span className="flex h-full flex-col gap-3 rounded-[20px] border-2 border-divider p-3 transition-colors group-hover:peer-not-checked:border-[color-mix(in_srgb,var(--brand)_45%,var(--divider))] peer-checked:border-brand peer-checked:bg-brand/9 peer-focus-visible:outline-3 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-brand motion-reduce:transition-none max-sheet:flex-row max-sheet:items-center">
              <span className={`flex h-21 flex-col gap-1.5 rounded-control border border-current/20 p-2.5 max-sheet:w-24 max-sheet:shrink-0 ${previews[value].surface}`} aria-hidden="true">
                <span className={`h-2.5 w-[55%] rounded-md ${previews[value].heading}`} />
                <span className={`h-[26px] rounded-md shadow-[0_1px_3px_rgb(0_0_0/12%)] ${previews[value].card}`} />
                <span className={`h-2 w-3/4 rounded-md ${previews[value].line}`} />
              </span>
              <span className="flex items-center justify-between gap-2 font-bold max-sheet:flex-1">
                {label}
                <span className="grid size-6 shrink-0 place-items-center rounded-full border-2 border-divider text-transparent group-has-checked:border-transparent group-has-checked:[background:var(--primary-fill)] group-has-checked:text-primary-foreground" aria-hidden="true"><Check className="size-3.5 stroke-3" /></span>
              </span>
            </span>
          </PulseChoiceControl>
        ))}
      </PulseCard>
    </section>
  );
}
