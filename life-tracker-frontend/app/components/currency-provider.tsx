"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { DEFAULT_CURRENCY, validCurrency } from "../lib/currency.mjs";

const CurrencyContext = createContext({ currency: DEFAULT_CURRENCY, setCurrency: (_code: string) => {} });
export const useCurrency = () => useContext(CurrencyContext);

export default function CurrencyProvider({ initialCurrency, children }: { initialCurrency: string; children: ReactNode }) {
  const [currency, select] = useState(initialCurrency);
  const generation = useRef(0);
  const currentCurrency = useRef(initialCurrency);
  const router = useRouter();
  const pathname = usePathname();
  const setCurrency = useCallback((code: string) => { if (!validCurrency(code)) return; generation.current++; currentCurrency.current = code; select(code); }, []);
  useEffect(() => { setCurrency(initialCurrency); }, [initialCurrency, setCurrency]);
  useEffect(() => {
    const controller = new AbortController();
    async function refresh() {
      const version = ++generation.current;
      if (pathname === "/") { currentCurrency.current = DEFAULT_CURRENCY; select(DEFAULT_CURRENCY); return; }
      try {
        const response = await fetch("/api/profile", { credentials: "include", cache: "no-store", signal: controller.signal });
        const profile = response.ok ? await response.json() : null;
        if (version !== generation.current || controller.signal.aborted) return;
        if (response.status === 401) { currentCurrency.current = DEFAULT_CURRENCY; select(DEFAULT_CURRENCY); return; }
        const code = profile?.currency ?? DEFAULT_CURRENCY;
        if (response.ok && validCurrency(code)) {
          const changed = currentCurrency.current !== code;
          currentCurrency.current = code; select(code);
          if (changed) router.refresh();
        }
      } catch { /* Keep the last confirmed preference on a temporary connection failure. */ }
    }
    void refresh();
    window.addEventListener("focus", refresh);
    return () => { controller.abort(); generation.current++; window.removeEventListener("focus", refresh); };
  }, [pathname, router]);
  return <CurrencyContext.Provider value={{ currency, setCurrency }}>{children}</CurrencyContext.Provider>;
}
