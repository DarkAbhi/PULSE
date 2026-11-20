"use client";

import { useId } from "react";

type Choice = { value: string; label: string; icon?: string };
type Props = { label: string; options: Choice[]; disabled?: boolean; columns?: boolean } & (
  { multiple?: false; value: string; onChange: (value: string) => void } |
  { multiple: true; value: string[]; onChange: (value: string[]) => void }
);

/** Native radio/checkbox behavior, including keyboard navigation and form semantics. */
export default function ChoiceGroup(props: Props) {
  const name = useId();
  return <fieldset disabled={props.disabled}>
    <legend className="sr-only">{props.label}</legend>
    <div className={`grid gap-3 ${props.columns ? "sm:grid-cols-2" : ""}`}>
      {props.options.map(option => {
        const checked = props.multiple ? props.value.includes(option.value) : props.value === option.value;
        return <label key={option.value} className={`flex min-h-16 cursor-pointer items-center gap-3 rounded-xl border px-4 py-4 transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-ring ${checked ? "border-action bg-accent text-accent-foreground" : "border-divider bg-card hover:border-action"}`}>
          <input className="size-5 shrink-0 accent-action" type={props.multiple ? "checkbox" : "radio"} name={name} value={option.value} checked={checked} onChange={() => {
            if (props.multiple) props.onChange(checked ? props.value.filter(v => v !== option.value) : [...props.value, option.value]);
            else props.onChange(option.value);
          }} />
          {option.icon && <span aria-hidden="true" className="text-2xl">{option.icon}</span>}
          <span className="text-sm font-medium sm:text-base">{option.label}</span>
        </label>;
      })}
    </div>
  </fieldset>;
}
