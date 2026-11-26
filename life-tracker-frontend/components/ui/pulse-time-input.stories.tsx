import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { useArgs } from "storybook/preview-api";
import { fn } from "storybook/test";
import PulseTimeInput from "@/components/ui/pulse-time-input";

const meta = {
  title: "Design System/Time Input",
  component: PulseTimeInput,
  decorators: [(Story) => <div className="max-w-sm"><Story /></div>],
  args: { label: "Workout reminder", value: "17:30", onChange: fn() },
  render: function Render(args) {
    const [, updateArgs] = useArgs<typeof args>();

    return <PulseTimeInput {...args} onChange={value => { updateArgs({ value }); args.onChange(value); }} />;
  },
} satisfies Meta<typeof PulseTimeInput>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};
export const Empty: Story = { args: { value: "" } };
export const Disabled: Story = { args: { disabled: true } };
