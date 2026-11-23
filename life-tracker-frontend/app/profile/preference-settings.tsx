"use client";

import { useEffect, useRef, useState, useTransition, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Check, type LucideIcon } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import { PulseChoiceControl } from "@/app/components/design-system/pulse-choice-group";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import { iconTileClass, labelClass, settingButtonClass, settingRowClass, settingValueClass } from "@/app/profile/profile-classes";

type PreferenceSettingsProps = {
  id: string;
  title: string;
  value: string;
  icon: LucideIcon;
  displayValue: string;
  detail?: string;
  choices: readonly string[];
  label: (value: string) => string;
  placeholder: string;
  description?: string;
  save: (value: string) => Promise<{ ok: true; value: string } | { ok: false; error: string }>;
  onSaved: (value: string) => void;
};

function normalize(text: string) {
  return text.replaceAll("_", " ").trim().toLowerCase();
}

export default function PreferenceSettings({
  id, title, value, icon: Icon, displayValue, detail, choices, label, placeholder, description, save, onSaved,
}: PreferenceSettingsProps) {
  const trigger = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [draft, setDraft] = useState(value);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [pending, startTransition] = useTransition();
  const router = useRouter();

  const dialogId = `${id}-dialog-title`;
  const plural = id === "currency" ? "currencies" : "time zones";
  const matches = isOpen ? choices.map(choice => ({ value: choice, label: label(choice) }))
    .filter(choice => normalize(choice.label).includes(normalize(search))) : [];

  useEffect(() => { setDraft(value); }, [value]);

  useEffect(() => {
    if (!isOpen) return;

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape" && !pending) {
        event.preventDefault();
        setIsOpen(false);
        trigger.current?.focus();
      }
      if (event.key !== "Tab") return;

      const dialog = document.querySelector(`[aria-labelledby="${dialogId}"]`);
      const items = dialog?.querySelectorAll<HTMLElement>('input:not(:disabled), button:not(:disabled)');
      if (!items?.length) return;
      const first = items[0];
      const last = items[items.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [dialogId, isOpen, pending]);

  function open() {
    setDraft(value);
    setSearch("");
    setError("");
    setMessage("");
    setIsOpen(true);
  }

  function close() {
    setIsOpen(false);
    trigger.current?.focus();
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending || draft === value || !matches.some(choice => choice.value === draft)) return;
    setError("");
    startTransition(async () => {
      const result = await save(draft);
      if (!result.ok) {
        setError(result.error);
        return;
      }
      onSaved(result.value);
      close();
      setMessage(`${title} saved.`);
      router.refresh();
    });
  }

  return (
    <div className={settingRowClass}>
      <span className={iconTileClass} aria-hidden="true"><Icon /></span>
      <div className="min-w-0">
        <p className={labelClass}>{title}</p>
        <p className={settingValueClass}>{displayValue}</p>
        {detail && <p className="text-[0.88rem] text-muted-foreground">{detail}</p>}
        <p role="status" className="text-sm text-success">{message}</p>
      </div>
      <PulseButton
        ref={trigger}
        type="button"
        variant="chip" size="lg" shape="pill" labelStyle="strong"
        className={settingButtonClass}
        onClick={open}
        aria-label={`Change ${title.toLowerCase()}`}
      >
        Change
      </PulseButton>

      {isOpen && (
        <PulseDialog labelledBy={dialogId}>
          <PulseDialogHeader id={dialogId} description={description} icon={<Icon className="size-5" />}>
            Change {title.toLowerCase()}
          </PulseDialogHeader>
          <form className="mt-5 space-y-4" onSubmit={submit}>
            <div>
              <label htmlFor={`${id}-search`} className="text-sm font-medium">Search {plural}</label>
              <input
                autoFocus
                id={`${id}-search`}
                type="search"
                value={search}
                disabled={pending}
                onChange={event => setSearch(event.target.value)}
                placeholder={placeholder}
                className="mt-1.5 w-full px-3.5"
              />
            </div>
            <div className="-mx-1 grid max-h-[min(340px,45dvh)] gap-1.5 overflow-y-auto p-1" role="radiogroup" aria-label={`Choose a ${title.toLowerCase()}`}>
              {matches.map(choice => (
                <PulseChoiceControl
                  key={choice.value}
                  type="radio"
                  name={`${id}-choice`}
                  value={choice.value}
                  checked={draft === choice.value}
                  disabled={pending}
                  onCheckedChange={() => { setDraft(choice.value); setError(""); }}
                >
                  <span className="flex min-h-14 items-center justify-between gap-3 rounded-2xl border-2 border-transparent bg-muted px-3.5 py-2 font-bold wrap-anywhere peer-checked:border-brand peer-checked:bg-[color-mix(in_srgb,var(--brand)_10%,var(--card))] peer-focus-visible:outline-2 peer-focus-visible:outline-offset-1 peer-focus-visible:outline-brand group-hover:peer-not-checked:border-divider peer-checked:[&_svg]:visible">
                    {choice.label}
                    <Check className="invisible size-5 shrink-0 text-brand" aria-hidden="true" />
                  </span>
                </PulseChoiceControl>
              ))}
              {!matches.length && <p className="text-[0.88rem] text-muted-foreground">No matching {plural}.</p>}
            </div>
            {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
            <PulseDialogActions>
              <PulseDialogAction type="button" variant="secondary" disabled={pending} onClick={close}>Cancel</PulseDialogAction>
              <PulseDialogAction type="submit" disabled={pending || draft === value || !matches.some(choice => choice.value === draft)}>
                {pending ? "Saving…" : `Save ${title.toLowerCase()}`}
              </PulseDialogAction>
            </PulseDialogActions>
          </form>
        </PulseDialog>
      )}
    </div>
  );
}
