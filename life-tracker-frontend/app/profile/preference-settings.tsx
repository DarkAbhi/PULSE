"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Button from "../components/design-system/button";
import PulseCard from "../components/design-system/pulse-card";
import Dialog, { DialogAction, DialogActions } from "../components/design-system/dialog";

type PreferenceSettingsProps = {
  id: string;
  title: string;
  value: string;
  choices: readonly string[];
  label: (value: string) => string;
  placeholder: string;
  description?: string;
  save: (value: string) => Promise<{ ok: true; value: string } | { ok: false; error: string }>;
  onSaved: (value: string) => void;
};

export default function PreferenceSettings({ id, title, value, choices, label, placeholder, description, save, onSaved }: PreferenceSettingsProps) {
  const trigger = useRef<HTMLButtonElement>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [draft, setDraft] = useState(value);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [pending, startTransition] = useTransition();
  const router = useRouter();

  useEffect(() => { setDraft(value); }, [value]);
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !pending) {
        event.preventDefault();
        setIsOpen(false);
        trigger.current?.focus();
      }
      if (event.key !== "Tab") return;
      const items = document.querySelector<HTMLElement>(`[aria-labelledby="${id}-dialog-title"]`)?.querySelectorAll<HTMLElement>('input:not(:disabled), select:not(:disabled), button:not(:disabled)');
      if (!items?.length) return;
      const first = items[0];
      const last = items[items.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [id, isOpen, pending]);
  const plural = id === "currency" ? "currencies" : "time zones";
  const normalize = (text: string) => text.replaceAll("_", " ").toLowerCase();
  const matches = choices.filter(choice => normalize(label(choice)).includes(normalize(search.trim())));

  function open() {
    setDraft(value);
    setSearch("");
    setError("");
    setMessage("");
    setIsOpen(true);
  }

  return <PulseCard as="section" aria-labelledby={`${id}-heading`} className="mt-6 w-full p-6 text-left sm:p-8">
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div className="min-w-0">
        <h2 id={`${id}-heading`} className="text-lg font-semibold">{title}</h2>
        <p className="mt-2 break-words text-sm text-muted-foreground">{label(value)}</p>
      </div>
      <Button ref={trigger} type="button" variant="secondary" onClick={open} aria-label={`Change ${title.toLowerCase()}`}>Change</Button>
    </div>
    {message && <p role="status" className="mt-3 text-sm text-success">{message}</p>}
    {isOpen && <Dialog labelledBy={`${id}-dialog-title`}>
      <div>
      <h2 id={`${id}-dialog-title`} className="text-lg font-semibold">Change {title.toLowerCase()}</h2>
      {description && <p className="mt-2 text-sm text-muted-foreground">{description}</p>}
      <form className="mt-5 space-y-4" onSubmit={event => {
        event.preventDefault(); setError("");
        startTransition(async () => {
          const result = await save(draft);
          if (!result.ok) { setError(result.error); return; }
          onSaved(result.value);
          setIsOpen(false);
          trigger.current?.focus();
          setMessage(`${title} saved.`);
          router.refresh();
        });
      }}>
        <div>
          <label htmlFor={`${id}-search`} className="text-sm font-medium">Search {plural}</label>
          <input autoFocus id={`${id}-search`} type="search" value={search} disabled={pending} onChange={event => setSearch(event.target.value)} placeholder={placeholder} className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2" />
        </div>
        <div>
          <label htmlFor={`${id}-choice`} className="text-sm font-medium">Choose a {title.toLowerCase()}</label>
          <select id={`${id}-choice`} size={6} value={matches.includes(draft) ? draft : ""} disabled={pending || !matches.length} onChange={event => { setDraft(event.target.value); setError(""); }} className="mt-2 w-full rounded-lg border border-border bg-background px-3 py-2">
            {!matches.length ? <option value="">No matching {plural}</option> : <><option value="" disabled>Select a {title.toLowerCase()}</option>{matches.map(choice => <option key={choice} value={choice}>{label(choice)}</option>)}</>}
          </select>
        </div>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        <DialogActions>
          <DialogAction type="button" variant="secondary" disabled={pending} onClick={() => { setIsOpen(false); trigger.current?.focus(); }}>Cancel</DialogAction>
          <DialogAction type="submit" disabled={pending || draft === value || !matches.includes(draft)}>{pending ? "Saving…" : `Save ${title.toLowerCase()}`}</DialogAction>
        </DialogActions>
      </form>
      </div>
    </Dialog>}
  </PulseCard>;
}


