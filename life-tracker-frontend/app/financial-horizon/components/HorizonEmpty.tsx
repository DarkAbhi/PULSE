import PulseCard, { type PulseCardProps } from "@/app/components/design-system/pulse-card";

export default function HorizonEmpty({ className = "", ...props }: PulseCardProps) {
  return (
    <PulseCard
      {...props}
      className={`border border-dashed border-divider bg-transparent p-8 text-center shadow-none! ${className}`}
    />
  );
}
