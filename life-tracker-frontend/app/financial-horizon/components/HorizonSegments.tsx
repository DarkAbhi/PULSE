import PulseButton from "@/app/components/design-system/pulse-button";

interface HorizonSegmentsProps<T extends string> {
  label: string;
  options: readonly { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
  className?: string;
}

export default function HorizonSegments<T extends string>({
  label,
  options,
  value,
  onChange,
  className = "",
}: HorizonSegmentsProps<T>) {
  return (
    <div role="group" aria-label={label} className={`inline-flex items-center gap-0.5 rounded-full bg-chip p-1 ${className}`}>
      {options.map(option => (
        <PulseButton
          key={option.value}
          type="button"
          size="sm"
          shape="pill"
          labelStyle="strong"
          variant="secondary"
          aria-pressed={value === option.value}
          className="min-h-9! bg-transparent! px-4! py-0! text-muted-foreground! aria-[pressed=false]:hover:bg-chip! aria-pressed:bg-card! aria-pressed:text-foreground! aria-pressed:shadow-card forced-colors:aria-pressed:outline-1 forced-colors:aria-pressed:outline-[ButtonText]"
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </PulseButton>
      ))}
    </div>
  );
}
