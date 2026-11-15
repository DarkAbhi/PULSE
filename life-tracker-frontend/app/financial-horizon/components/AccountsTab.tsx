"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import Button from "@/app/components/design-system/button";
import Dialog, { DialogAction, DialogActions } from "@/app/components/design-system/dialog";
import Select from "@/app/components/design-system/select";
import { listAccounts, saveAccount, setAccountArchived } from "../accounts";
import type { Account, AccountFields } from "../account-types";

const fieldClass = "mt-1 block w-full rounded-lg border border-divider bg-card px-3 py-2";

export default function AccountsTab({ currency }: { currency: string }) {
  const [accounts, setAccounts] = useState<Account[] | null>(null);
  const [editor, setEditor] = useState<{ id: number | null; fields: AccountFields } | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    setError("");
    const response = await listAccounts(true);
    if (response.ok) setAccounts(response.data.accounts);
    else setError(response.error);
  }, []);
  useEffect(() => { void load(); }, [load]);

  function open(account?: Account) {
    setError("");
    const fields: AccountFields = account ? {
      display_name: account.display_name,
      institution_name: account.institution_name,
      account_type: account.account_type,
      bank_account_subtype: account.bank_account_subtype,
      currency: account.currency,
      country_code: account.country_code,
      last_four: account.last_four,
    } : {
      display_name: "", institution_name: "", account_type: "bank_account",
      bank_account_subtype: "", currency, country_code: "", last_four: "",
    };
    setEditor({ id: account?.id ?? null, fields });
  }

  function update(fields: Partial<AccountFields>) {
    setEditor(current => current && ({ ...current, fields: { ...current.fields, ...fields } }));
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editor || pending) return;
    setPending(true);
    setError("");
    const response = await saveAccount(editor.id, editor.fields);
    if (response.ok) {
      setEditor(null);
      await load();
    } else setError(response.error);
    setPending(false);
  }

  async function archive(account: Account) {
    setPending(true);
    setError("");
    const response = await setAccountArchived(account.id, !account.archived_at);
    if (response.ok) await load();
    else setError(response.error);
    setPending(false);
  }

  const fields = editor?.fields;

  return (
    <section className="space-y-4 rounded-2xl bg-card p-4 sm:p-6" aria-labelledby="accounts-heading">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="accounts-heading" className="text-lg font-semibold">Financial accounts</h2>
        <Button variant="primary" disabled={pending} onClick={() => open()}>Add account</Button>
      </header>
      {!editor && error && <p role="alert">{error}</p>}
      {!accounts && (error ? <Button onClick={load}>Retry</Button> : <p role="status">Loading accounts…</p>)}
      {accounts?.length === 0 && <p className="text-muted-foreground">Add a bank account or credit card to assign transactions to it.</p>}
      <ul className="divide-y divide-divider">
        {accounts?.map(account => (
          <li key={account.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
            <div>
              <p className="font-medium">{account.display_name}</p>
              <p className="text-sm text-muted-foreground">
                {account.institution_name} · {account.account_type === "credit_card" ? "Credit card" : "Bank account"}
                {account.last_four && ` · •••• ${account.last_four}`}
                {` · ${account.currency}`}
                {account.archived_at && " · Archived"}
              </p>
            </div>
            <div className="flex gap-2">
              <Button disabled={pending} onClick={() => open(account)} aria-label={`Edit ${account.display_name}`}>Edit</Button>
              <Button disabled={pending} onClick={() => archive(account)} aria-label={`${account.archived_at ? "Restore" : "Archive"} ${account.display_name}`}>
                {account.archived_at ? "Restore" : "Archive"}
              </Button>
            </div>
          </li>
        ))}
      </ul>
      {fields && (
        <Dialog labelledBy="account-editor-title">
          <h3 id="account-editor-title" className="mb-4 text-xl font-semibold">{editor.id === null ? "Add account" : "Edit account"}</h3>
          <form className="space-y-3" onSubmit={save}>
            <label className="block">Account name<input className={fieldClass} required maxLength={100} value={fields.display_name} onChange={e => update({ display_name: e.target.value })} /></label>
            <label className="block">Institution<input className={fieldClass} required maxLength={150} value={fields.institution_name} onChange={e => update({ institution_name: e.target.value })} /></label>
            <label className="block">Account type
              <Select value={fields.account_type} onChange={e => update({ account_type: e.target.value as AccountFields["account_type"], bank_account_subtype: "" })}>
                <option value="bank_account">Bank account</option>
                <option value="credit_card">Credit card</option>
              </Select>
            </label>
            {fields.account_type === "bank_account" && (
              <label className="block">Bank account subtype
                <Select value={fields.bank_account_subtype} onChange={e => update({ bank_account_subtype: e.target.value as AccountFields["bank_account_subtype"] })}>
                  <option value="">Unspecified</option>
                  <option value="savings">Savings</option>
                  <option value="current">Current</option>
                  <option value="other">Other</option>
                </Select>
              </label>
            )}
            <label className="block">Currency<input className={fieldClass} readOnly value={fields.currency} /></label>
            <div>
              <label className="block" htmlFor="account-country-code">Country code (optional)</label>
              <p id="account-country-code-hint" className="text-sm text-muted-foreground">Use two letters, such as IN for India.</p>
              <input
                id="account-country-code"
                aria-describedby="account-country-code-hint"
                className={fieldClass}
                maxLength={2}
                pattern="[A-Za-z]{2}"
                title="Enter a two-letter country code, such as IN for India."
                value={fields.country_code}
                onChange={e => update({ country_code: e.target.value.toUpperCase() })}
              />
            </div>
            <label className="block">Last four digits (optional)<input className={fieldClass} maxLength={4} pattern="[0-9]{4}" inputMode="numeric" value={fields.last_four} onChange={e => update({ last_four: e.target.value })} /></label>
            {error && <p role="alert">{error}</p>}
            <DialogActions>
              <Button type="button" disabled={pending} onClick={() => { setEditor(null); setError(""); }}>Cancel</Button>
              <DialogAction disabled={pending}>{pending ? "Saving…" : "Save"}</DialogAction>
            </DialogActions>
          </form>
        </Dialog>
      )}
    </section>
  );
}
