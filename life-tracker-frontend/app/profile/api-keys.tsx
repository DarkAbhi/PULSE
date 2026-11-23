"use client";

import { formatInstant } from "@/app/lib/time-zone.mjs";

import { useTimeZone } from "@/app/components/time-zone-provider";

import { useEffect, useRef, useState } from "react";
import { Copy, KeyRound, Plus, Trash2 } from "lucide-react";
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
  const [busy, setBusy] = useState(false);
  const [revokeKey, setRevokeKey] = useState<APIKey | null>(null);
  const tokenInput = useRef<HTMLInputElement>(null);

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
    setIsOpen(false);
    setName("");
    setToken("");
    setError("");
  }

  async function createKey(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const response = await fetch("/api/profile/api-keys", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ name: name.trim() }),
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
    if (!tokenInput.current) return;
    if (await copyApiKey(token, tokenInput.current)) setError("");
    else setError("Could not copy the key. Select and copy it from the field.");
  }

  return (
    <section className={styles.section} aria-labelledby="profile-api-keys">
      <header className={styles.sectionHeader}>
        <div>
          <h2 id="profile-api-keys"><KeyRound aria-hidden="true" />API keys</h2>
          <p>Create keys so external services can access your Pulse data. Treat them like passwords.</p>
        </div>
        <Button
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
        <Dialog labelledBy="api-key-dialog-title">
          <DialogHeader
            id="api-key-dialog-title"
            icon={<KeyRound className="size-5" />}
            description={token ? "Copy it now. It will not be shown again." : "Give this key a name so you can recognize it later."}
            onClose={closeDialog}
          >
            {token ? "Save your API key" : "Create API key"}
          </DialogHeader>
          {error && <p className="mt-4 text-sm text-destructive" role="alert">{error}</p>}
          {token ? (
            <div className="mt-6">
              <label htmlFor="created-api-key" className="mb-2 block text-xs font-semibold uppercase tracking-wider text-muted-foreground">Your API key</label>
              <div className="flex gap-2">
                <input ref={tokenInput} id="created-api-key" readOnly value={token} onFocus={(event) => event.target.select()} className="min-w-0 flex-1 rounded-xl bg-secondary px-3 py-2 text-xs text-foreground" />
                <Button type="button" variant="secondary" size="sm" aria-label="Copy API key" onClick={copyKey}><Copy className="h-4 w-4" /></Button>
              </div>
              <DialogActions><DialogAction type="button" onClick={closeDialog}>Done</DialogAction></DialogActions>
            </div>
          ) : (
            <form className="mt-6" onSubmit={createKey}>
              <label htmlFor="api-key-name" className="mb-2 block text-xs font-semibold uppercase tracking-wider text-muted-foreground">Name</label>
              <input id="api-key-name" value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required autoFocus placeholder="e.g. Personal sync service" className="w-full rounded-xl bg-secondary px-4 py-2.5 text-sm text-foreground" />
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
