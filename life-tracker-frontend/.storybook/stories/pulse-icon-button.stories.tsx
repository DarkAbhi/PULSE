import { Plus, Trash2 } from "lucide-react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { fn } from "storybook/test";
import PulseIconButton, { type PulseIconButtonVariant } from "@/app/components/design-system/pulse-icon-button";

const variants = ["plain", "secondary", "overlay", "danger"] satisfies PulseIconButtonVariant[];

const meta = {
  title: "Design System/Icon Button",
  component: PulseIconButton,
  args: {
    "aria-label": "Add",
    variant: "plain",
    size: "md",
    onClick: fn(),
    children: <Plus aria-hidden="true" className="size-4" />,
  },
  argTypes: {
    variant: { control: "inline-radio", options: variants },
    size: { control: "inline-radio", options: ["sm", "md", "lg", "xl"] },
    disabled: { control: "boolean" },
    children: { control: false },
  },
} satisfies Meta<typeof PulseIconButton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Playground: Story = {};

export const Disabled: Story = { args: { disabled: true } };

export const Variants: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <div className="flex flex-wrap items-center gap-3">
      {variants.map(variant => {
        const Icon = variant === "danger" ? Trash2 : Plus;
        const button = (
          <PulseIconButton key={variant} type="button" variant={variant} aria-label={variant} title={variant}>
            <Icon aria-hidden="true" className="size-4" />
          </PulseIconButton>
        );
        return variant === "overlay"
          ? <div key={variant} className="rounded-full bg-(image:--primary-gradient) text-white">{button}</div>
          : button;
      })}
    </div>
  ),
};
