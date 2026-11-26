"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { DEFAULT_CURRENCY, validCurrency } from "@/lib/currency/currency.mjs";
import { DEFAULT_TIME_ZONE, validTimeZone } from "@/lib/time-zone/time-zone.mjs";

const CurrencyContext = createContext({ currency: DEFAULT_CURRENCY, setCurrency: (_code: string) => {} });
const TimeZoneContext = createContext({ timeZone: DEFAULT_TIME_ZONE, setTimeZone: (_zone: string) => {} });
export const useCurrency = () => useContext(CurrencyContext);
export const useTimeZone = () => useContext(TimeZoneContext);

export default function ProfilePreferencesProvider({ initialCurrency, initialTimeZone, children }: {
  initialCurrency: string;
  initialTimeZone: string;
  children: ReactNode;
}) {
  const [currency, selectCurrency] = useState(initialCurrency);
  const [timeZone, selectTimeZone] = useState(initialTimeZone);
  const current = useRef({ currency: initialCurrency, timeZone: initialTimeZone });
  const generation = useRef(0);
  const router = useRouter();
  const signedInRoute = usePathname() !== "/";

  const setCurrency = useCallback((code: string) => {
    if (!validCurrency(code)) return;
    generation.current++;
    current.current.currency = code;
    selectCurrency(code);
  }, []);
  const setTimeZone = useCallback((zone: string) => {
    if (!validTimeZone(zone)) return;
    generation.current++;
    current.current.timeZone = zone;
    selectTimeZone(zone);
  }, []);

  useEffect(() => { setCurrency(initialCurrency); }, [initialCurrency, setCurrency]);
  useEffect(() => { setTimeZone(initialTimeZone); }, [initialTimeZone, setTimeZone]);

  useEffect(() => {
    if (!signedInRoute) {
      setCurrency(DEFAULT_CURRENCY);
      setTimeZone(DEFAULT_TIME_ZONE);
      return;
    }

    const controller = new AbortController();
    async function refresh() {
      const version = ++generation.current;
      try {
        const response = await fetch("/api/profile", { cache: "no-store", signal: controller.signal });
        const profile = response.ok ? await response.json() : null;
        if (controller.signal.aborted || version !== generation.current) return;
        if (!response.ok && response.status !== 401) return;

        const code = profile?.currency ?? DEFAULT_CURRENCY;
        const zone = profile?.timezone ?? DEFAULT_TIME_ZONE;
        const changesCurrency = validCurrency(code) && current.current.currency !== code;
        const changesTimeZone = validTimeZone(zone) && current.current.timeZone !== zone;
        if (changesCurrency) setCurrency(code);
        if (changesTimeZone) setTimeZone(zone);
        if (response.ok && (changesCurrency || changesTimeZone)) router.refresh();
      } catch {
        // Keep the latest confirmed preferences through temporary connection failures.
      }
    }
    window.addEventListener("focus", refresh);
    return () => {
      controller.abort();
      window.removeEventListener("focus", refresh);
    };
  }, [router, setCurrency, setTimeZone, signedInRoute]);

  const currencyValue = useMemo(() => ({ currency, setCurrency }), [currency, setCurrency]);
  const timeZoneValue = useMemo(() => ({ timeZone, setTimeZone }), [timeZone, setTimeZone]);
  return (
    <TimeZoneContext.Provider value={timeZoneValue}>
      <CurrencyContext.Provider value={currencyValue}>{children}</CurrencyContext.Provider>
    </TimeZoneContext.Provider>
  );
}
