import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import PulseCard, { PulseCardLink } from "@/app/components/design-system/pulse-card";

const meta = {
  title: "Design System/Card",
  component: PulseCard,
  args: { as: "section", className: "max-w-sm p-6" },
} satisfies Meta<typeof PulseCard>;

export default meta;
type Story = StoryObj<typeof meta>;

function CardContent() {
  return (
    <>
      <h2 className="text-lg font-bold">Today’s workout</h2>
      <p className="mt-1 text-sm text-muted-foreground">Upper body · 6 exercises</p>
    </>
  );
}

export const Surface: Story = { args: { children: <CardContent /> } };

export const LinkedCard: Story = {
  parameters: { controls: { disable: true } },
  render: () => (
    <PulseCardLink href="/fitness" className="block max-w-sm p-6">
      <CardContent />
    </PulseCardLink>
  ),
};
