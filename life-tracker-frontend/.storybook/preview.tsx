import { useLayoutEffect, type ReactNode } from "react";
import { Bricolage_Grotesque } from "next/font/google";
import type { Preview } from "@storybook/nextjs-vite";
import "@/app/globals.css";

const bricolageGrotesque = Bricolage_Grotesque({
  variable: "--font-bricolage-grotesque",
  subsets: ["latin"],
  display: "swap",
});

function PulsePreview({ dark, children }: { dark: boolean; children: ReactNode }) {
  useLayoutEffect(() => {
    const root = document.documentElement;
    root.classList.toggle("dark", dark);
    root.classList.add(...bricolageGrotesque.variable.split(/\s+/));
    document.body.classList.add("antialiased");
  }, [dark]);

  return children;
}

const preview: Preview = {
  tags: ["autodocs"],
  globalTypes: {
    pulseTheme: {
      toolbar: {
        title: "Pulse theme",
        icon: "paintbrush",
        items: [{ value: "light", title: "Light" }, { value: "dark", title: "Dark" }],
      },
    },
  },
  initialGlobals: { pulseTheme: "light" },
  parameters: {
    backgrounds: { disable: true },
    nextjs: { appDirectory: true },
    viewport: {
      options: {
        phone: { name: "Phone", styles: { width: "390px", height: "844px" }, type: "mobile" },
        tablet: { name: "Tablet", styles: { width: "800px", height: "1024px" }, type: "tablet" },
        desktop: { name: "Desktop", styles: { width: "1440px", height: "900px" }, type: "desktop" },
      },
    },
  },
  decorators: [
    (Story, { globals }) => (
      <PulsePreview dark={globals.pulseTheme === "dark"}>
        <Story />
      </PulsePreview>
    ),
  ],
};

export default preview;
