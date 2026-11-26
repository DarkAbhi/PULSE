import PulseButton, { type PulseButtonProps } from "@/components/ui/pulse-button";

type HorizonActionProps = Omit<PulseButtonProps, "size" | "labelStyle">;

export default function HorizonAction({
  variant = "primary",
  type = "button",
  className = "",
  ...props
}: HorizonActionProps) {
  return (
    <PulseButton
      {...props}
      type={type}
      variant={variant}
      size="lg"
      labelStyle="strong"
      className={`whitespace-nowrap enabled:active:scale-95 [&_svg]:size-5 [&_svg]:shrink-0 ${className}`}
    />
  );
}
