import { useState } from "react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { fn } from "storybook/test";
import PulseChoiceGroup, { PulseChoiceControl } from "@/components/ui/pulse-choice-group";

const workoutOptions = [
  { value: "strength", label: "Strength" },
  { value: "cardio", label: "Cardio" },
  { value: "mobility", label: "Mobility" },
];

const meta = {
  title: "Design System/Choice Group",
  component: PulseChoiceGroup,
  subcomponents: { PulseChoiceControl },
  decorators: [(Story) => <div className="max-w-xl"><Story /></div>],
  argTypes: { multiple: { control: false }, value: { control: false } },
} satisfies Meta<typeof PulseChoiceGroup>;

export default meta;
type Story = StoryObj<typeof meta>;

export const SingleSelection: Story = {
  args: { label: "Workout type", options: workoutOptions, value: "strength", onChange: fn(), columns: true },
  render: function SingleSelection(args) {
    const [value, setValue] = useState("strength");

    return <PulseChoiceGroup {...args} multiple={false} value={value} onChange={setValue} />;
  },
};

export const MultipleSelection: Story = {
  args: { label: "Workout types", options: workoutOptions, multiple: true, value: ["strength"], onChange: fn(), columns: true },
  render: function MultipleSelection(args) {
    const [value, setValue] = useState(["strength"]);

    return <PulseChoiceGroup {...args} multiple value={value} onChange={setValue} />;
  },
};

export const CustomControl: Story = {
  args: { label: "Quest tracking", options: workoutOptions, value: "strength", onChange: fn() },
  parameters: { controls: { disable: true } },
  render: function CustomControl() {
    const [checked, setChecked] = useState(false);

    return (
      <PulseChoiceControl type="checkbox" name="quest" value="workout" checked={checked} onCheckedChange={setChecked}>
        <span className="inline-block rounded-full border border-divider bg-card px-4 py-2 peer-checked:border-action peer-checked:bg-accent peer-focus-visible:outline-2 peer-focus-visible:outline-ring">
          Track workouts
        </span>
      </PulseChoiceControl>
    );
  },
};
