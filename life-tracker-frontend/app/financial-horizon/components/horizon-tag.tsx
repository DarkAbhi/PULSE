import type { ReactNode } from "react";

interface HorizonTagProps {
  tone?: "neutral" | "dim" | "good" | "bad" | "warn";
  emphasis?: boolean;
  className?: string;
  children: ReactNode;
}

const tones = {
  neutral: "bg-chip",
  dim: "bg-chip text-muted-foreground",
  good: "bg-success/15 text-success",
  bad: "bg-destructive/15 text-destructive",
  warn: "bg-(--finance-warning-fill)/25 text-warning data-[emphasis=true]:bg-(--finance-warning-fill)/30",
};

export default function HorizonTag({
  tone = "neutral",
  emphasis = false,
  className = "",
  children,
}: HorizonTagProps) {
  const weight = emphasis ? "px-3 font-bold" : "px-2.5 font-semibold";

  return (
    <span data-tone={tone} data-emphasis={emphasis} className={`inline-flex items-center gap-1.5 rounded-full py-1 text-caption ${weight} ${tones[tone]} ${className}`}>
      {children}
    </span>
  );
}
