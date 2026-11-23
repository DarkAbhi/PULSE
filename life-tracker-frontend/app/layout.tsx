import type { Metadata } from "next";
import { Bricolage_Grotesque } from "next/font/google";
import { ThemeProvider } from "next-themes";
import "@/app/globals.css";
import ProfilePreferencesProvider from "@/app/components/profile-preferences-provider";
import { getUserCurrency } from "@/app/lib/user-currency";
import { getUserTimeZone } from "@/app/lib/user-time-zone";

const bricolageGrotesque = Bricolage_Grotesque({
  variable: "--font-bricolage-grotesque",
  subsets: ["latin"],
  display: "swap",
});

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
