"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function RefreshQuests({ resetAt }: { resetAt: string }) {
  const router = useRouter();
  useEffect(() => {
    const delay = Date.parse(resetAt) - Date.now();
    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") router.refresh();
    };
    const timer = window.setTimeout(refreshWhenVisible, Math.max(1000, delay + 500));
    document.addEventListener("visibilitychange", refreshWhenVisible);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [resetAt, router]);
  return null;
}
