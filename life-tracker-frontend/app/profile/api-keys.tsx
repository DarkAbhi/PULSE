"use client";

import { useEffect, useState } from "react";
import { Copy, KeyRound, Plus, Trash2, X } from "lucide-react";
import Button from "../components/design-system/button";
import Dialog, { DialogAction, DialogActions } from "../components/design-system/dialog";
import ConfirmationDialog from "../components/design-system/confirmation-dialog";

type APIKey = {
  id: number;
  name: string;
  token_prefix: string;
  created_at: string;
};

export default function APIKeys() {
  const [keys, setKeys] = useState<APIKey[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [name, setName] = useState("");
  const [token, setToken] = useState("");
  const [busy, setBusy] = useState(false);
  const [revokeKey, setRevokeKey] = useState<APIKey | null>(null);

  useEffect(() => {
    let active = true;
    fetch("/api/profile/api-keys", { credentials: "include", cache: "no-store" })
      .then(async (response) => {
        if (!response.ok) throw new Error("Could not load API keys.");
        return (await response.json()) as APIKey[];
      })
      .then((items) => { if (active) setKeys(items); })
      .catch(() => { if (active) setError("Could not load API keys."); })
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

  return (
    <section className="mt-6 w-full rounded-2xl border border-border bg-card p-6 text-left shadow-sm sm:p-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-foreground"><KeyRound className="h-5 w-5" /> API Keys</h2>
          <p className="mt-1 text-xs text-muted-foreground">Create keys for external services to access your data. Treat each key like a password.</p>
        </div>
        <Button size="sm" icon={<Plus className="h-4 w-4" />} onClick={() => { setError(""); setIsOpen(true); }}>Create key</Button>
      </div>
      {error && !isOpen && !revokeKey && <p className="mt-4 text-sm text-destructive" role="alert">{error}</p>}
      {loading ? <p className="mt-6 text-sm text-muted-foreground">Loading keys…</p> : keys.length === 0 ? (
        <p className="mt-6 text-sm text-muted-foreground">No API keys yet.</p>
      ) : (
        <ul className="mt-5 divide-y divide-border">
          {keys.map((key) => (
            <li className="flex items-center justify-between gap-4 py-3" key={key.id}>
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-foreground">{key.name}</p>
                <p className="text-xs text-muted-foreground">{key.token_prefix}… · Created {new Date(key.created_at).toLocaleDateString()}</p>
              </div>
              <Button variant="iconDanger" size="sm" aria-label={`Revoke ${key.name}`} onClick={() => { setError(""); setRevokeKey(key); }}><Trash2 className="h-4 w-4" /></Button>
            </li>
          ))}
        </ul>
      )}
      {isOpen && (
        <Dialog labelledBy="api-key-dialog-title">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 id="api-key-dialog-title" className="text-xl font-bold text-foreground">{token ? "Save your API key" : "Create API key"}</h2>
              <p className="mt-1 text-sm text-muted-foreground">{token ? "Copy it now. It will not be shown again." : "Give this key a name so you can recognize it later."}</p>
            </div>
            <Button variant="icon" size="sm" aria-label="Close" onClick={closeDialog}><X className="h-4 w-4" /></Button>
          </div>
          {error && <p className="mt-4 text-sm text-destructive" role="alert">{error}</p>}
          {token ? (
            <div className="mt-6">
              <label htmlFor="created-api-key" className="mb-2 block text-xs font-semibold uppercase tracking-wider text-muted-foreground">Your API key</label>
              <div className="flex gap-2">
                <input id="created-api-key" readOnly value={token} onFocus={(event) => event.target.select()} className="min-w-0 flex-1 rounded-xl border border-border bg-background px-3 py-2 font-mono text-xs text-foreground" />
                <Button variant="secondary" size="sm" aria-label="Copy API key" onClick={() => navigator.clipboard.writeText(token).catch(() => setError("Could not copy the key. Select and copy it from the field."))}><Copy className="h-4 w-4" /></Button>
              </div>
              <DialogActions><DialogAction type="button" onClick={closeDialog}>Done</DialogAction></DialogActions>
            </div>
          ) : (
            <form className="mt-6" onSubmit={createKey}>
              <label htmlFor="api-key-name" className="mb-2 block text-xs font-semibold uppercase tracking-wider text-muted-foreground">Name</label>
              <input id="api-key-name" value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required autoFocus placeholder="e.g. Personal sync service" className="w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/20" />
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
