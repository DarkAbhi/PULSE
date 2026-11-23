"use client";

import { Check, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import { cardClass, sectionClass, sectionDescriptionClass, sectionHeadingClass } from "./profile-classes";

const options = [
  { value: "light", label: "Light" },
  { value: "dark", label: "Dark" },
  { value: "system", label: "System" },
] as const;

const previews = {
  light: { surface: "bg-[#f6f7ff]", heading: "bg-[#1a1740]", card: "bg-white", line: "bg-[#c9cce6]" },
  dark: { surface: "bg-[#0d0a1f]", heading: "bg-[#f4f1ff]", card: "bg-[#1a1636]", line: "bg-[#3a3560]" },
  system: {
    surface: "bg-[linear-gradient(115deg,#f6f7ff_50%,#0d0a1f_50%)]",
    heading: "bg-[linear-gradient(90deg,#1a1740_50%,#f4f1ff_50%)]",
    card: "bg-[linear-gradient(90deg,#fff_50%,#1a1636_50%)]",
    line: "bg-[linear-gradient(90deg,#c9cce6_50%,#3a3560_50%)]",
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
      <div className={`${cardClass} grid flex-1 gap-3 p-6 min-[761px]:grid-cols-3`} role="radiogroup" aria-labelledby="profile-theme" aria-describedby="profile-theme-help">
        {options.map(({ value, label }) => (
          <label key={value} className="group relative block min-w-0 cursor-pointer">
            <input className="peer absolute inset-0 size-full cursor-pointer opacity-0" type="radio" name="theme" value={value} checked={mounted && theme === value} onChange={() => setTheme(value)} />
            <span className="flex h-full flex-col gap-3 rounded-[20px] border-2 border-(--profile-line) p-3 transition-colors group-hover:peer-not-checked:border-[color-mix(in_srgb,var(--profile-accent)_45%,var(--profile-line))] peer-checked:border-(--profile-accent) peer-checked:bg-(--profile-accent)/9 peer-focus-visible:outline-3 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-(--profile-accent) motion-reduce:transition-none max-[761px]:flex-row max-[761px]:items-center">
              <span className={`flex h-[84px] flex-col gap-1.5 rounded-[14px] border border-[#7f7f7f]/25 p-2.5 max-[761px]:w-24 max-[761px]:shrink-0 ${previews[value].surface}`} aria-hidden="true">
                <span className={`h-2.5 w-[55%] rounded-md ${previews[value].heading}`} />
                <span className={`h-[26px] rounded-md shadow-[0_1px_3px_rgb(0_0_0/12%)] ${previews[value].card}`} />
                <span className={`h-2 w-3/4 rounded-md ${previews[value].line}`} />
              </span>
              <span className="flex items-center justify-between gap-2 font-bold max-[761px]:flex-1">
                {label}
                <span className="grid size-6 shrink-0 place-items-center rounded-full border-2 border-(--profile-line) text-transparent group-has-checked:border-transparent group-has-checked:[background:var(--profile-primary)] group-has-checked:text-(--profile-primary-ink)" aria-hidden="true"><Check className="size-3.5 stroke-3" /></span>
              </span>
            </span>
          </label>
        ))}
      </div>
    </section>
  );
}
