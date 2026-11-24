import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import PulseCard, { PulseCardLink } from "@/app/components/design-system/pulse-card";

const meta = {
  title: "Design System/Card",
  component: PulseCard,
  args: { as: "section", variant: "surface", elevation: "raised", size: "default", className: "max-w-sm p-6" },
  argTypes: {
    variant: { control: "inline-radio", options: ["surface", "tile"] },
    elevation: { control: "inline-radio", options: ["raised", "flat"] },
    size: { control: "inline-radio", options: ["default", "compact"] },
  },
} satisfies Meta<typeof PulseCard>;

export default meta;
type Story = StoryObj<typeof meta>;

function CardContent({ muted = true }: { muted?: boolean }) {
  return (
    <>
      <h2 className="text-lg font-bold">Today’s workout</h2>
      <p className={`mt-1 text-sm ${muted ? "text-muted-foreground" : "opacity-85"}`}>Upper body · 6 exercises</p>
    </>
  );
}

export const Surface: Story = { args: { children: <CardContent /> } };

export const Flat: Story = { args: { elevation: "flat", children: <CardContent /> } };

export const Compact: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <PulseCard as="section" className="max-w-sm p-4">
      <PulseCard size="compact" elevation="flat" className="p-4 [--card-fill:var(--chip)]">
        <CardContent />
      </PulseCard>
    </PulseCard>
  ),
};

export const Tile: Story = {
  args: {
    variant: "tile",
    className: "max-w-sm p-6 [--tile-start:var(--brand-indigo)] [--tile-end:var(--brand-purple)]",
    children: <CardContent muted={false} />,
  },
};

export const LinkedCard: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <PulseCardLink href="/fitness" className="block max-w-sm p-6">
      <CardContent />
    </PulseCardLink>
  ),
};
