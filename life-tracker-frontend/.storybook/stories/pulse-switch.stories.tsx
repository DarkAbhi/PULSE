import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { useArgs } from "storybook/preview-api";
import { fn } from "storybook/test";
import PulseSwitch from "@/app/components/design-system/pulse-switch";

const meta = {
  title: "Design System/Switch",
  component: PulseSwitch,
  decorators: [(Story) => <div className="max-w-sm"><Story /></div>],
  args: { checked: true, children: "Track workouts", onChange: fn() },
  argTypes: { children: { control: "text" } },
  render: function Render(args) {
    const [, updateArgs] = useArgs<typeof args>();

    return <PulseSwitch {...args} onChange={checked => { updateArgs({ checked }); args.onChange(checked); }} />;
  },
} satisfies Meta<typeof PulseSwitch>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Enabled: Story = {};
export const Unchecked: Story = { args: { checked: false } };
export const Disabled: Story = { args: { disabled: true } };
