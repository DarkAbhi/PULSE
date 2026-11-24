import { Plus } from "lucide-react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { fn } from "storybook/test";
import PulseButton, { type PulseButtonVariant } from "@/app/components/design-system/pulse-button";

const textVariants = [
  "primary", "secondary", "tertiary", "soft", "chip", "outline",
  "filledDanger", "onHero",
] satisfies PulseButtonVariant[];

const meta = {
  title: "Design System/Button",
  component: PulseButton,
  args: { children: "Save", variant: "primary", size: "md", onClick: fn() },
  argTypes: {
    variant: { control: "select", options: textVariants },
    size: { control: "inline-radio", options: ["sm", "md", "lg", "xl", "touch"] },
    disabled: { control: "boolean" },
    children: { control: "text" },
  },
} satisfies Meta<typeof PulseButton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Playground: Story = {};

export const WithIcon: Story = {
  args: { children: "Add workout", icon: <Plus aria-hidden="true" className="size-4" /> },
};

export const Disabled: Story = { args: { disabled: true } };

export const Variants: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <div className="flex max-w-3xl flex-wrap items-center gap-3">
      {textVariants.map(variant => (
        <PulseButton key={variant} type="button" variant={variant}>{variant}</PulseButton>
      ))}
    </div>
  ),
};
