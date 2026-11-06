"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { usePathname, useRouter } from "next/navigation";
import { DEFAULT_TIME_ZONE, validTimeZone } from "@/app/lib/time-zone.mjs";

const TimeZoneContext = createContext({ timeZone: DEFAULT_TIME_ZONE, setTimeZone: (_zone: string) => {} });
export const useTimeZone = () => useContext(TimeZoneContext);

export default function TimeZoneProvider({ initialTimeZone, children }: { initialTimeZone: string; children: ReactNode }) {
  const [timeZone, select] = useState(initialTimeZone);
  const generation = useRef(0);
  const currentZone = useRef(initialTimeZone);
  const router = useRouter();
  const pathname = usePathname();
  const setTimeZone = useCallback((zone: string) => { generation.current++; currentZone.current = zone; select(zone); }, []);
  useEffect(() => { setTimeZone(initialTimeZone); }, [initialTimeZone, setTimeZone]);
  useEffect(() => {
    const controller = new AbortController();
    async function refresh() {
      const version = ++generation.current;
      if (pathname === "/") { currentZone.current = DEFAULT_TIME_ZONE; select(DEFAULT_TIME_ZONE); return; }
      try {
        const response = await fetch("/api/profile", { credentials: "include", cache: "no-store", signal: controller.signal });
        const profile = response.ok ? await response.json() : null;
        if (version !== generation.current || controller.signal.aborted) return;
        if (response.status === 401) { currentZone.current = DEFAULT_TIME_ZONE; select(DEFAULT_TIME_ZONE); return; }
        const zone = profile?.timezone ?? DEFAULT_TIME_ZONE;
        if (response.ok && validTimeZone(zone)) {
          const changed = currentZone.current !== zone;
          currentZone.current = zone; select(zone);
          if (changed) router.refresh();
        }
      } catch { /* Keep the last confirmed preference on a temporary connection failure. */ }
    }
    void refresh();
    window.addEventListener("focus", refresh);
    return () => { controller.abort(); generation.current++; window.removeEventListener("focus", refresh); };
  }, [pathname, router]);
  return <TimeZoneContext.Provider value={{ timeZone, setTimeZone }}>{children}</TimeZoneContext.Provider>;
}
