import PulseButton, { type PulseButtonProps } from "@/app/components/design-system/pulse-button";

type HorizonActionProps = Omit<PulseButtonProps, "size" | "shape" | "labelStyle">;

export default function HorizonAction({
  variant = "gradient",
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
      shape="pill"
      labelStyle="strong"
      className={`whitespace-nowrap enabled:active:scale-95 [&_svg]:size-5 [&_svg]:shrink-0 ${className}`}
    />
  );
}
