"use client";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/time-zone-provider";

import { useEffect, useRef, useState } from "react";
import { Check, Copy, Info, KeyRound, Plus, Trash2 } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import { copyApiKey } from "@/app/profile/copy-api-key";
import PulseCard from "@/app/components/design-system/pulse-card";
import { iconTileClass, labelClass, sectionClass, sectionDescriptionClass, sectionHeadingClass } from "@/app/profile/profile-classes";

type APIKey = {
  id: number;
  name: string;
  token_prefix: string;
  created_at: string;
};

const keyTableHeadingClass = `${labelClass} px-6 py-3`;
const keyTableCellClass = "h-[72px] px-6 py-3 max-sheet:h-auto max-sheet:min-w-0 max-sheet:p-0";

export default function APIKeys() {
  const { timeZone } = useTimeZone();
  const [keys, setKeys] = useState<APIKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [name, setName] = useState("");
  const [token, setToken] = useState("");
  const [copied, setCopied] = useState(false);
  const [busy, setBusy] = useState(false);
  const [revokeKey, setRevokeKey] = useState<APIKey | null>(null);
  const tokenInput = useRef<HTMLTextAreaElement>(null);
  const createTrigger = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    let active = true;
    fetch("/api/profile/api-keys", { credentials: "include", cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) throw new Error("Could not load API keys.");
        return (await response.json()) as APIKey[];
      })
      .then((items) => { if (active) setKeys(items); })
      .catch(() => { if (active) setLoadError("Could not load API keys."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  function closeDialog() {
    if (busy) return;
    setIsOpen(false);
    setName("");
    setToken("");
    setCopied(false);
    setError("");
    createTrigger.current?.focus();
  }

  async function createKey(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const keyName = name.trim();
    if (!keyName) {
      setError("Enter a name for this key.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const response = await fetch("/api/profile/api-keys", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ name: keyName }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.error ?? "Could not create API key.");
      const { token: createdToken, ...key } = data as APIKey & { token: string };
      setKeys((current) => [key, ...current]);
      setToken(createdToken);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not create API key.");
    } finally {
      setBusy(false);
    }
  }

  async function revoke() {
    if (!revokeKey) return;
    setBusy(true);
    setError("");
    try {
      const response = await fetch(`/api/profile/api-keys/${revokeKey.id}`, {
        method: "DELETE",
        credentials: "include",
      });
      if (!response.ok) throw new Error("Could not revoke API key.");
      setKeys((current) => current.filter((key) => key.id !== revokeKey.id));
      setRevokeKey(null);
    } catch {
      setError("Could not revoke API key.");
    } finally {
      setBusy(false);
    }
  }

  async function copyKey() {
    const input = tokenInput.current;
    if (!input) return;
    const didCopy = await copyApiKey(token, input);
    if (tokenInput.current !== input) return;
    setCopied(didCopy);
    setError(didCopy ? "" : "Could not copy the key. Select and copy it from the field.");
  }

  return (
    <section className={sectionClass} aria-labelledby="profile-api-keys">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 id="profile-api-keys" className={sectionHeadingClass}><KeyRound aria-hidden="true" />API keys</h2>
          <p className={sectionDescriptionClass}>Create keys so external services can access your Pulse data. Treat them like passwords.</p>
        </div>
        <PulseButton
          ref={createTrigger}
          type="button"
          variant="gradient" size="lg" shape="pill" labelStyle="strong"
          disabled={loading}
          icon={<Plus className="size-5" aria-hidden="true" />}
          onClick={() => { setError(""); setIsOpen(true); }}
        >
          Create API key
        </PulseButton>
      </header>
      {loading || keys.length === 0 ? (
        <PulseCard className="grid justify-items-center gap-2.5 px-6 py-9 text-center text-muted-foreground shadow-card">
          <span className={iconTileClass} aria-hidden="true"><KeyRound /></span>
          {loading ? <p role="status">Loading keys…</p> : !loadError && (
            <>
              <strong className="text-foreground">No API keys yet</strong>
              <p>Create a key to let an external service access your Pulse data.</p>
            </>
          )}
          {loadError && <p className="text-destructive" role="alert">{loadError}</p>}
        </PulseCard>
      ) : (
        <PulseCard className="overflow-hidden shadow-card">
          {error && !isOpen && !revokeKey && <p className="px-6 py-3 text-sm text-destructive" role="alert">{error}</p>}
          <table role="table" className="w-full table-fixed border-collapse text-left max-sheet:block" aria-labelledby="profile-api-keys">
            <thead role="rowgroup" className="bg-muted max-sheet:sr-only">
              <tr role="row">
                <th scope="col" className={keyTableHeadingClass}>Name</th>
                <th scope="col" className={keyTableHeadingClass}>Key</th>
                <th scope="col" className={`${keyTableHeadingClass} w-[174px]`}>Created</th>
                <th scope="col" className={`${keyTableHeadingClass} w-[92px]`}><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody role="rowgroup" className="divide-y divide-divider max-sheet:block">
              {keys.map((key) => (
                <tr key={key.id} role="row" className="max-sheet:grid max-sheet:grid-cols-[minmax(0,1fr)_auto] max-sheet:items-center max-sheet:gap-x-4 max-sheet:gap-y-2.5 max-sheet:px-6 max-sheet:py-3.5">
                  <td role="cell" className={`${keyTableCellClass} max-sheet:col-span-full`}>
                    <div className="flex items-center gap-3 font-bold wrap-anywhere">
                      <span className="grid size-9 shrink-0 place-items-center rounded-xl bg-chip text-muted-foreground" aria-hidden="true"><KeyRound className="size-4.5" /></span>
                      <span>{key.name}</span>
                    </div>
                  </td>
                  <td role="cell" className={`${keyTableCellClass} max-sheet:col-span-full`}><code className="inline-block rounded-[10px] bg-muted px-2.5 py-[5px] text-sm wrap-anywhere">{key.token_prefix}…</code></td>
                  <td role="cell" className={`${keyTableCellClass} tabular-nums`}>
                    <span className="text-caption font-bold text-muted-foreground uppercase sheet:hidden max-sheet:block" aria-hidden="true">Created</span>
                    <time dateTime={key.created_at}>{formatInstant(key.created_at, timeZone, { dateStyle: "medium" })}</time>
                  </td>
                  <td role="cell" className={`${keyTableCellClass} text-right`}>
                    <PulseButton
                      type="button"
                      variant="iconChipDanger"
                      size="lg"
                      shape="pill"
                      aria-label={`Revoke ${key.name}`}
                      onClick={() => { setError(""); setRevokeKey(key); }}
                    >
                      <Trash2 className="size-5" aria-hidden="true" />
                    </PulseButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </PulseCard>
      )}
      {isOpen && (
        <PulseDialog labelledBy="api-key-dialog-title" onDismiss={busy ? undefined : closeDialog}>
          <PulseDialogHeader id="api-key-dialog-title">
            {token ? "Your new API key" : "Create API key"}
          </PulseDialogHeader>
          {token ? (
            <div className="mt-4 space-y-4">
              <p className="flex gap-3 rounded-2xl bg-warning-soft px-4 py-3.5 text-[0.92rem]">
                <Info className="mt-0.5 size-5 shrink-0 text-warning" aria-hidden="true" />
                <span>Copy this key now. For your security, it won’t be shown again.</span>
              </p>
              <div className="flex items-center gap-2.5 rounded-2xl border-[1.5px] border-dashed border-divider bg-secondary p-3.5">
                <label htmlFor="created-api-key" className="sr-only">Your API key</label>
                <textarea
                  ref={tokenInput}
                  id="created-api-key"
                  className="min-h-0! min-w-0 flex-1 field-sizing-content resize-none rounded-none! border-0! font-mono text-sm wrap-anywhere"
                  readOnly
                  rows={2}
                  value={token}
                  onFocus={event => event.target.select()}
                />
                <PulseButton type="button" variant="chip" size="lg" shape="pill" autoFocus className="shrink-0" onClick={copyKey}>
                  {copied ? <Check className="size-4.5" aria-hidden="true" /> : <Copy className="size-4.5" aria-hidden="true" />}
                  {copied ? "Copied" : "Copy"}
                </PulseButton>
              </div>
              <span className="sr-only" role="status">{copied ? "API key copied" : ""}</span>
              {error && <p className="text-sm font-semibold text-destructive" role="alert">{error}</p>}
              <PulseDialogActions><PulseDialogAction type="button" onClick={closeDialog}>Done</PulseDialogAction></PulseDialogActions>
            </div>
          ) : (
            <form className="mt-4 space-y-4" onSubmit={createKey}>
              <div>
                <label htmlFor="api-key-name" className="mb-1.5 block text-sm font-bold text-muted-foreground">Key name</label>
                <input
                  id="api-key-name"
                  className="w-full px-3.5"
                  value={name}
                  onChange={event => { setName(event.target.value); setError(""); }}
                  maxLength={120}
                  required
                  autoFocus
                  autoComplete="off"
                  disabled={busy}
                  placeholder="e.g. Home Assistant"
                  aria-describedby="api-key-name-hint"
                />
              </div>
              <p id="api-key-name-hint" className="text-sm text-muted-foreground">Give it a name so you can recognise it later.</p>
              {error && <p className="text-sm font-semibold text-destructive" role="alert">{error}</p>}
              <PulseDialogActions>
                <PulseDialogAction type="button" variant="secondary" onClick={closeDialog} disabled={busy}>Cancel</PulseDialogAction>
                <PulseDialogAction type="submit" disabled={busy}>{busy ? "Creating…" : "Create key"}</PulseDialogAction>
              </PulseDialogActions>
            </form>
          )}
        </PulseDialog>
      )}
      <PulseConfirmationDialog
        isOpen={revokeKey !== null}
        onClose={() => { if (!busy) setRevokeKey(null); }}
        onConfirm={revoke}
        title="Revoke API key?"
        description={`“${revokeKey?.name ?? ""}” will stop working immediately.`}
        confirmText="Revoke key"
        isLoading={busy}
        error={revokeKey ? error : undefined}
      />
    </section>
  );
}
