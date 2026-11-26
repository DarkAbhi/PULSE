import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import PulseCard from "@/components/ui/pulse-card";
import { frame } from "@/components/layout/page-frame";

const meta = {
  title: "Design System/Page Frame",
  parameters: { layout: "fullscreen", controls: { disable: true } },
} satisfies Meta;

export default meta;
type Story = StoryObj<typeof meta>;

export const Layout: Story = {
  render: () => (
    <main className={frame.page}>
      <div className={frame.content}>
        <PulseCard as="section" className="p-6">
          <h2 className="text-xl font-bold">Page content</h2>
          <p className="mt-1 text-muted-foreground">The shared frame sets the page gutters and maximum content width.</p>
        </PulseCard>
      </div>
    </main>
  ),
};
