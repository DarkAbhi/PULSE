import { ChevronDown, Plus, Settings2 } from "lucide-react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { fn } from "storybook/test";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseDropdown, { PulseDropdownItem, PulseDropdownSeparator } from "@/app/components/design-system/pulse-dropdown";

const meta = {
  title: "Design System/Dropdown",
  component: PulseDropdown,
  parameters: { layout: "centered" },
  args: { trigger: () => null, children: null },
} satisfies Meta<typeof PulseDropdown>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: () => (
    <div className="min-h-80">
      <PulseDropdown
        trigger={({ isOpen, ...triggerProps }) => (
          <PulseButton {...triggerProps} type="button" size="lg" labelStyle="strong" iconPosition="end"
            icon={<ChevronDown aria-hidden="true" className={`size-5 transition-transform motion-reduce:transition-none ${isOpen ? "rotate-180" : ""}`} />}>
            Quick action
          </PulseButton>
        )}
      >
        <PulseDropdownItem icon={<Plus aria-hidden="true" className="size-4" />} description="Record daily expense/income" onSelect={fn()}>
          Log transaction
        </PulseDropdownItem>
        <PulseDropdownSeparator />
        <PulseDropdownItem icon={<Settings2 aria-hidden="true" className="size-4" />} onSelect={fn()}>
          Settings
        </PulseDropdownItem>
      </PulseDropdown>
    </div>
  ),
};
