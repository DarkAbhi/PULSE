import type { Meta, StoryObj } from "@storybook/nextjs-vite";
import { fn } from "storybook/test";
import PulseAppBar, {
  appBarControl,
  PulseAppBarBell,
  PulseAppBarNotificationLink,
} from "@/components/layout/pulse-app-bar";

const meta = {
  title: "Design System/App Bar",
  component: PulseAppBar,
  decorators: [(Story) => <div className="max-w-3xl"><Story /></div>],
  args: {
    backLabel: "Back to dashboard",
    back: { href: "/dashboard" },
    notifications: <PulseAppBarNotificationLink />,
    onProfilePage: false,
  },
  argTypes: { back: { control: false }, notifications: { control: false } },
} satisfies Meta<typeof PulseAppBar>;

export default meta;
type Story = StoryObj<typeof meta>;

export const LinkNavigation: Story = {};

export const UnreadNotifications: Story = {
  args: {
    notifications: (
      <button type="button" aria-label="Unread notifications" className={appBarControl} onClick={fn()}>
        <PulseAppBarBell hasUnread />
      </button>
    ),
  },
};

export const CallbackNavigation: Story = { args: { back: { onClick: fn() } } };
