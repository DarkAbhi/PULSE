import type { Metadata } from "next";
import { Bricolage_Grotesque } from "next/font/google";
import { ThemeProvider } from "next-themes";
import "@/app/globals.css";
import ProfilePreferencesProvider from "@/providers/profile-preferences-provider";
import { dashboardViewStorageKey } from "@/features/dashboard";
import { getUserCurrency } from "@/lib/user/user-currency";
import { getUserTimeZone } from "@/lib/user/user-time-zone";

const bricolageGrotesque = Bricolage_Grotesque({
  variable: "--font-bricolage-grotesque",
  subsets: ["latin"],
  display: "swap",
});

// Browser storage is available before the dashboard's server-rendered content is painted.
const initializeDashboardView = `try {
  if (localStorage.getItem(${JSON.stringify(dashboardViewStorageKey)}) === "compact") {
    document.documentElement.dataset.initialDashboardView = "compact";
  }
} catch {}`;

export const metadata: Metadata = {
  title: "Pulse",
  description: "Track workouts, meals, vehicles and finances in Pulse.",
  icons: {
    icon: "/favicon.ico",
  },
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const [timeZone, currency] = await Promise.all([getUserTimeZone(), getUserCurrency()]);
  return (
    <html lang="en" className={bricolageGrotesque.variable} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: initializeDashboardView }} />
      </head>
      <body className="antialiased">
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          <a href="#main-content" className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-lg focus:bg-card focus:p-3 focus:text-foreground">
            Skip to main content
          </a>
          <ProfilePreferencesProvider initialTimeZone={timeZone} initialCurrency={currency}>
            {children}
          </ProfilePreferencesProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
