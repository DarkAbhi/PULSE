import { useId } from "react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { fn } from "storybook/test";
import PulseSelect from "@/components/ui/pulse-select";

const meta = {
  title: "Design System/Select",
  component: PulseSelect,
  args: {
    defaultValue: "",
    className: "min-h-12 w-full rounded-lg border border-divider bg-card px-3 py-2 text-foreground",
    onChange: fn(),
    children: (
      <>
        <option value="" disabled>Select a workout type</option>
        <option value="strength">Strength</option>
        <option value="cardio">Cardio</option>
        <option value="mobility">Mobility</option>
      </>
    ),
  },
  argTypes: { children: { control: false }, defaultValue: { control: false }, value: { control: false } },
  render: function Render(args) {
    const id = useId();

    return (
      <div className="grid max-w-sm gap-2">
        <label htmlFor={id} className="font-medium">Workout type</label>
        <PulseSelect {...args} id={id} key={String(args.defaultValue)} />
      </div>
    );
  },
} satisfies Meta<typeof PulseSelect>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Placeholder: Story = {};
export const Populated: Story = { args: { defaultValue: "strength" } };
export const Disabled: Story = { args: { defaultValue: "strength", disabled: true } };
