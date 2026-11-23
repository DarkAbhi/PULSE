"use client";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/time-zone-provider";

import { useEffect, useRef, useState } from "react";
import { Check, Copy, Info, KeyRound, Plus, Trash2 } from "lucide-react";
import Button from "@/app/components/design-system/button";
import Dialog, { DialogAction, DialogActions, DialogHeader } from "@/app/components/design-system/dialog";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import { copyApiKey } from "./copy-api-key";
import styles from "./profile.module.css";

type APIKey = {
  id: number;
  name: string;
  token_prefix: string;
  created_at: string;
};

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
    <section className={styles.section} aria-labelledby="profile-api-keys">
      <header className={styles.sectionHeader}>
        <div>
          <h2 id="profile-api-keys"><KeyRound aria-hidden="true" />API keys</h2>
          <p>Create keys so external services can access your Pulse data. Treat them like passwords.</p>
        </div>
        <Button
          ref={createTrigger}
          type="button"
          className={`${styles.pill} ${styles.primary}`}
          disabled={loading}
          icon={<Plus className="size-5" aria-hidden="true" />}
          onClick={() => { setError(""); setIsOpen(true); }}
        >
          Create API key
        </Button>
      </header>
      {loading || keys.length === 0 ? (
        <div className={`${styles.card} ${styles.empty}`}>
          <span className={styles.tile} aria-hidden="true"><KeyRound /></span>
          {loading ? <p role="status">Loading keys…</p> : !loadError && (
            <>
              <strong>No API keys yet</strong>
              <p>Create a key to let an external service access your Pulse data.</p>
            </>
          )}
          {loadError && <p className="text-destructive" role="alert">{loadError}</p>}
        </div>
      ) : (
        <div className={`${styles.card} ${styles.keyCard}`}>
          {error && !isOpen && !revokeKey && <p className="px-6 py-3 text-sm text-destructive" role="alert">{error}</p>}
          <table role="table" className={styles.keyTable} aria-labelledby="profile-api-keys">
            <thead role="rowgroup">
              <tr role="row">
                <th scope="col">Name</th>
                <th scope="col">Key</th>
                <th scope="col">Created</th>
                <th scope="col"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody role="rowgroup">
              {keys.map((key) => (
                <tr key={key.id} role="row">
                  <td role="cell">
                    <div className={styles.keyName}>
                      <span className={styles.keyIcon} aria-hidden="true"><KeyRound className="size-[18px]" /></span>
                      <span>{key.name}</span>
                    </div>
                  </td>
                  <td role="cell"><code>{key.token_prefix}…</code></td>
                  <td role="cell" className="tabular-nums">
                    <span className={styles.createdLabel} aria-hidden="true">Created</span>
                    <time dateTime={key.created_at}>{formatInstant(key.created_at, timeZone, { dateStyle: "medium" })}</time>
                  </td>
                  <td role="cell">
                    <Button
                      type="button"
                      variant="iconDanger"
                      size="lg"
                      className={styles.revoke}
                      aria-label={`Revoke ${key.name}`}
                      onClick={() => { setError(""); setRevokeKey(key); }}
                    >
                      <Trash2 className="size-5" aria-hidden="true" />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {isOpen && (
        <Dialog labelledBy="api-key-dialog-title" onDismiss={busy ? undefined : closeDialog}>
          <DialogHeader id="api-key-dialog-title">
            {token ? "Your new API key" : "Create API key"}
          </DialogHeader>
          {token ? (
            <div className="mt-4 space-y-4">
              <p className="flex gap-3 rounded-2xl bg-[#ffc83d]/22 px-4 py-3.5 text-[0.92rem]">
                <Info className="mt-0.5 size-5 shrink-0 text-warning" aria-hidden="true" />
                <span>Copy this key now. For your security, it won’t be shown again.</span>
              </p>
              <div className="flex items-center gap-2.5 rounded-2xl border-[1.5px] border-dashed border-divider bg-secondary p-3.5">
                <label htmlFor="created-api-key" className="sr-only">Your API key</label>
                <textarea
                  ref={tokenInput}
                  id="created-api-key"
                  className="min-h-0! min-w-0 flex-1 field-sizing-content resize-none rounded-none! border-0! font-mono text-[0.9rem] wrap-anywhere"
                  readOnly
                  rows={2}
                  value={token}
                  onFocus={event => event.target.select()}
                />
                <Button type="button" variant="secondary" autoFocus className="min-h-11! shrink-0 rounded-full! bg-(--horizon-chip)! px-3.5! text-[0.9rem]! hover:bg-(--horizon-line)!" onClick={copyKey}>
                  {copied ? <Check className="size-[18px]" aria-hidden="true" /> : <Copy className="size-[18px]" aria-hidden="true" />}
                  {copied ? "Copied" : "Copy"}
                </Button>
              </div>
              <span className="sr-only" role="status">{copied ? "API key copied" : ""}</span>
              {error && <p className="text-[0.9rem] font-semibold text-destructive" role="alert">{error}</p>}
              <DialogActions><DialogAction type="button" onClick={closeDialog}>Done</DialogAction></DialogActions>
            </div>
          ) : (
            <form className="mt-4 space-y-4" onSubmit={createKey}>
              <div>
                <label htmlFor="api-key-name" className="mb-1.5 block text-[0.88rem] font-bold text-muted-foreground">Key name</label>
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
              <p id="api-key-name-hint" className="text-[0.9rem] text-muted-foreground">Give it a name so you can recognise it later.</p>
              {error && <p className="text-[0.9rem] font-semibold text-destructive" role="alert">{error}</p>}
              <DialogActions>
                <DialogAction type="button" variant="secondary" onClick={closeDialog} disabled={busy}>Cancel</DialogAction>
                <DialogAction type="submit" disabled={busy}>{busy ? "Creating…" : "Create key"}</DialogAction>
              </DialogActions>
            </form>
          )}
        </Dialog>
      )}
      <ConfirmationDialog
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
