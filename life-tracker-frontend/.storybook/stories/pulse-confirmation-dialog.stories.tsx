import { useState } from "react";
import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { fn } from "storybook/test";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";

const meta = {
  title: "Design System/Confirmation Dialog",
  component: PulseConfirmationDialog,
  args: {
    isOpen: false,
    title: "Delete workout?",
    description: "This will delete the workout and all its logged sets.",
    confirmText: "Delete",
    variant: "destructive",
    onClose: fn(),
    onConfirm: fn(),
  },
  argTypes: {
    variant: { options: ["destructive", "positive", "amber"], control: "inline-radio" },
    description: { control: "text" },
    isOpen: { control: false },
    isLoading: { control: false },
  },
  render: function Render(args) {
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);

    const close = () => {
      args.onClose();
      setOpen(false);
    };

    const confirm = async () => {
      setLoading(true);
      try {
        await args.onConfirm();
        setOpen(false);
      } finally {
        setLoading(false);
      }
    };

    return (
      <>
        <PulseButton type="button" onClick={() => setOpen(true)}>Open confirmation</PulseButton>
        <PulseConfirmationDialog {...args} isOpen={open} isLoading={loading} onClose={close} onConfirm={confirm} />
      </>
    );
  },
} satisfies Meta<typeof PulseConfirmationDialog>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Destructive: Story = {};

export const Positive: Story = {
  args: {
    title: "Finish workout?",
    description: "Save the logged sets and mark the workout complete.",
    confirmText: "Finish",
    variant: "positive",
  },
};

export const Error: Story = { args: { error: "The workout couldn’t be deleted. Try again." } };

export const Loading: Story = {
  args: {
    confirmLoadingText: "Deleting…",
    onConfirm: fn(() => new Promise<void>(resolve => setTimeout(resolve, 1500))),
  },
};
