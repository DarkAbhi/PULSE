"use client";

import { useSyncExternalStore } from "react";
import { dashboardViewStorageKey } from "@/app/lib/dashboard-view-storage";

type DashboardView = "large" | "compact";

const changeEvent = "pulse-dashboard-view-change";
let inMemoryView: DashboardView = "large";

function getView(): DashboardView {
  try {
    return window.localStorage.getItem(dashboardViewStorageKey) === "compact" ? "compact" : "large";
  } catch {
    return inMemoryView;
  }
}

function getServerView(): undefined {
  return undefined;
}

function subscribe(onChange: () => void) {
  function onStorage(event: StorageEvent) {
    if (event.key === dashboardViewStorageKey || event.key === null) onChange();
  }

  window.addEventListener(changeEvent, onChange);
  window.addEventListener("storage", onStorage);
  return () => {
    window.removeEventListener(changeEvent, onChange);
    window.removeEventListener("storage", onStorage);
  };
}

export function setDashboardView(value: string) {
  if (value !== "large" && value !== "compact") return;
  inMemoryView = value;
  try {
    window.localStorage.setItem(dashboardViewStorageKey, value);
  } catch {
    // Keep the selection for this tab when browser storage is blocked.
  }
  window.dispatchEvent(new Event(changeEvent));
}

export function useDashboardView() {
  return useSyncExternalStore(subscribe, getView, getServerView);
}
