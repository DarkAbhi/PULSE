"use client";

import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import { Sun, Moon, Monitor } from "lucide-react";

export default function ThemeSettings() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  // Avoid hydration mismatch by only rendering after mount
  useEffect(() => {
    setMounted(true);
  }, []);

  const options = [
    { value: "light", label: "Light", Icon: Sun },
    { value: "dark", label: "Dark", Icon: Moon },
    { value: "system", label: "System", Icon: Monitor },
  ] as const;

  return (
    <div className="mt-6 w-full rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8 text-left">
      <h2 className="text-lg font-semibold text-foreground mb-1">Theme Settings</h2>
      <p className="text-xs text-muted-foreground mb-6">
        Choose how Life Tracker looks on your device.
      </p>
      <div className="grid grid-cols-3 gap-3">
        {options.map(({ value, label, Icon }) => (
          <button
            key={value}
            type="button"
            onClick={() => setTheme(value)}
            className={`flex flex-col items-center justify-center gap-2 rounded-xl border p-4 text-sm font-semibold transition cursor-pointer ${
              mounted && theme === value
                ? "border-primary bg-primary/10 text-primary"
                : "border-border bg-transparent text-muted-foreground hover:bg-muted hover:text-foreground"
            }`}
          >
            <Icon className="h-5 w-5" />
            <span>{label}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
