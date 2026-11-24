import type { ComponentPropsWithRef } from "react";

const selectClass =
  "min-w-0 max-w-full " +
  "disabled:cursor-not-allowed disabled:opacity-50 " +
  "normal-colors:appearance-none normal-colors:pe-9 " +
  "normal-colors:bg-[url(data:image/svg+xml,%3Csvg%20xmlns=%27http://www.w3.org/2000/svg%27%20viewBox=%270%200%2016%2016%27%20fill=%27none%27%3E%3Cpath%20d=%27m4%206%204%204%204-4%27%20stroke=%27%23827b70%27%20stroke-width=%271.5%27%20stroke-linecap=%27round%27%20stroke-linejoin=%27round%27/%3E%3C/svg%3E)] " +
  "normal-colors:bg-position-[right_0.75rem_center] normal-colors:bg-no-repeat normal-colors:bg-size-[1rem] " +
  "normal-colors:rtl:bg-position-[left_0.75rem_center]";

export default function PulseSelect({ className = "", ...props }: ComponentPropsWithRef<"select">) {
  return <select {...props} className={`${selectClass} ${className}`} />;
}
