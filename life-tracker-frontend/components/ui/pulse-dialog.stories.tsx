import { useId, useState } from "react";
import { Settings2 } from "lucide-react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import PulseButton from "@/components/ui/pulse-button";
import PulseDialog, {
  PulseDialogAction,
  PulseDialogActions,
  PulseDialogHeader,
} from "@/components/ui/pulse-dialog";

const meta = {
  title: "Design System/Dialog",
  component: PulseDialog,
  subcomponents: { PulseDialogHeader, PulseDialogActions, PulseDialogAction },
  args: { size: "sm", children: "Set when Pulse should remind you to work out." },
  argTypes: {
    size: { control: "select", options: ["sm", "md", "lg", "xl"] },
    children: { control: "text" },
    labelledBy: { control: false },
  },
  render: function Render(args) {
    const titleId = useId();
    const [open, setOpen] = useState(false);
    const close = () => setOpen(false);

    return (
      <>
        <PulseButton type="button" onClick={() => setOpen(true)}>Open dialog</PulseButton>
        {open && (
          <PulseDialog {...args} labelledBy={titleId} onDismiss={close}>
            <PulseDialogHeader id={titleId} icon={<Settings2 className="size-5" />} onClose={close}>
              Workout reminder
            </PulseDialogHeader>
            <p className="mt-4 text-sm text-muted-foreground">{args.children}</p>
            <PulseDialogActions>
              <PulseDialogAction variant="secondary" type="button" onClick={close}>Cancel</PulseDialogAction>
              <PulseDialogAction type="button" onClick={close}>Save</PulseDialogAction>
            </PulseDialogActions>
          </PulseDialog>
        )}
      </>
    );
  },
} satisfies Meta<typeof PulseDialog>;

export default meta;
type Story = StoryObj<typeof meta>;

export const DesktopDialog: Story = {};

export const PhoneBottomSheet: Story = {
  globals: { viewport: { value: "phone", isRotated: false } },
};
