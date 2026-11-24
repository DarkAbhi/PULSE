import { useState } from "react";
import { CalendarDays, LayoutGrid, Receipt, Target } from "lucide-react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import PulseTabs, { type PulseTabsVariant } from "@/app/components/design-system/pulse-tabs";

const sections = [
  { value: "overview", label: "Overview", icon: <LayoutGrid /> },
  { value: "budgets", label: "Budgets", icon: <Target /> },
  { value: "transactions", label: "Transactions", icon: <Receipt /> },
  { value: "obligations", label: "Obligations", icon: <CalendarDays /> },
];

const types = [
  { value: "all", label: "All Types" },
  { value: "debit", label: "Debit" },
  { value: "credit", label: "Credit" },
];

function Controlled({ variant, items, className }: { variant: PulseTabsVariant; items: typeof types; className?: string }) {
  const [value, setValue] = useState(items[0].value);
  return <PulseTabs label="Preview tabs" variant={variant} items={items} value={value} onChange={setValue} className={className} />;
}

const meta = {
  title: "Design System/Tabs",
  component: PulseTabs,
  args: { label: "Sections", items: sections, value: "overview", onChange: () => {}, variant: "primary" },
  argTypes: { variant: { control: "inline-radio", options: ["primary", "segmented"] } },
} satisfies Meta<typeof PulseTabs>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Primary: Story = {
  render: () => <Controlled variant="primary" items={sections} className="sm:grid-cols-4" />,
};

export const Segmented: Story = {
  render: () => <Controlled variant="segmented" items={types} />,
};
