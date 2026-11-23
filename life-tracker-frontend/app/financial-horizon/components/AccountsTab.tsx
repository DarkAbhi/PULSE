"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import HorizonAction from "@/app/financial-horizon/components/HorizonAction";
import Button from "@/app/components/design-system/button";
import PulseCard from "@/app/components/design-system/pulse-card";
import { Icon } from "@/app/financial-horizon/components/HorizonIcon";
import SectionHeader from "@/app/financial-horizon/components/SectionHeader";
import HorizonEmpty from "@/app/financial-horizon/components/HorizonEmpty";
import Dialog, { DialogAction, DialogActions, DialogHeader } from "@/app/components/design-system/dialog";
import Select from "@/app/components/design-system/select";
import { listAccounts, saveAccount, setAccountArchived } from "@/app/financial-horizon/accounts";
import type { Account, AccountFields } from "@/app/financial-horizon/account-types";

const networkLabels = { "": "Choose network", visa: "Visa", mastercard: "Mastercard", american_express: "American Express", diners_club: "Diners Club", rupay: "RuPay" };

const fieldClass = "mt-1 block w-full px-3 py-2";

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
      account_number: account.account_number, holder_name: account.holder_name,
      holder_email: account.holder_email, card_type: account.card_type,
      card_network: account.card_network, credit_limit: account.credit_limit,
      statement_day: account.statement_day,
      last_four: account.last_four,
    } : {
      display_name: "", institution_name: "", account_type: "bank_account",
      bank_account_subtype: "savings", last_four: "", account_number: "", holder_name: "",
      holder_email: "", card_type: "", card_network: "", credit_limit: null, statement_day: null,
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
    <PulseCard as="section" className="space-y-4 p-6" aria-labelledby="accounts-heading">
      <SectionHeader id="accounts-heading" title="Financial accounts" icon="bank">
        <HorizonAction disabled={pending} onClick={() => open()}><Icon name="plus" /> Add account</HorizonAction>
      </SectionHeader>
      {!editor && error && <p role="alert">{error}</p>}
      {!accounts && (error ? <HorizonAction variant="chip" onClick={load}>Retry</HorizonAction> : <p role="status">Loading accounts…</p>)}
      {accounts?.length === 0 && <HorizonEmpty as="p" className="text-muted-foreground">Add a bank account or credit card to assign transactions to it.</HorizonEmpty>}
      <ul className="divide-y divide-divider">
        {accounts?.map(account => (
          <li key={account.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
            <div>
              <p className="font-medium">{account.display_name}</p>
              <p className="text-sm text-muted-foreground">
                {account.institution_name} · {account.account_type === "credit_card" ? "Credit card" : "Bank account"}
                {account.last_four && ` · •••• ${account.last_four}`}
                {account.card_type && ` · ${account.card_type}`}
                {account.card_network && ` · ${networkLabels[account.card_network]}`}
                {account.archived_at && " · Archived"}
              </p>
            </div>
            <div className="flex gap-2">
              <Button variant="iconChip" size="lg" shape="pill" disabled={pending} onClick={() => open(account)} aria-label={`Edit ${account.display_name}`}><Icon name="edit" className="size-5" /></Button>
              <HorizonAction variant="outline" disabled={pending} onClick={() => archive(account)} aria-label={`${account.archived_at ? "Restore" : "Archive"} ${account.display_name}`}>
                {account.archived_at ? "Restore" : "Archive"}
              </HorizonAction>
            </div>
          </li>
        ))}
      </ul>
      {fields && (
        <Dialog labelledBy="account-editor-title">
          <DialogHeader id="account-editor-title">{editor.id === null ? "Add account" : "Edit account"}</DialogHeader>
          <form className="mt-4 space-y-3" onSubmit={save}>
            <label className="block">Account name<input className={fieldClass} required maxLength={100} value={fields.display_name} onChange={e => update({ display_name: e.target.value })} /></label>
            <label className="block">Institution<input className={fieldClass} required maxLength={150} value={fields.institution_name} onChange={e => update({ institution_name: e.target.value })} /></label>
            <div>
              <label className="block" htmlFor="financial-account-type">Account type</label>
              <Select id="financial-account-type" value={fields.account_type} onChange={e => update({ account_type: e.target.value as AccountFields["account_type"], bank_account_subtype: e.target.value === "bank_account" ? "savings" : "", account_number: "", holder_email: "", card_type: "", card_network: "", credit_limit: null, statement_day: null, last_four: "" })}>
                <option value="bank_account">Bank account</option>
                <option value="credit_card">Credit card</option>
              </Select>
            </div>
            {fields.account_type === "bank_account" && (
              <div>
                <label className="block" htmlFor="financial-bank-type">Bank account type</label>
                <Select id="financial-bank-type" required value={fields.bank_account_subtype} onChange={e => update({ bank_account_subtype: e.target.value as AccountFields["bank_account_subtype"] })}>
                  <option value="" disabled>Choose account type</option>
                  <option value="savings">Savings</option>
                  <option value="current">Current</option>
                </Select>
              </div>
            )}
            <label className="block">{fields.account_type === "credit_card" ? "Cardholder name" : "Account holder name"}<input className={fieldClass} required maxLength={150} autoComplete="name" value={fields.holder_name} onChange={e => update({ holder_name: e.target.value })} /></label>
            {fields.account_type === "bank_account" ? <>
              <label className="block">Account holder email<input className={fieldClass} type="email" required maxLength={254} autoComplete="email" value={fields.holder_email} onChange={e => update({ holder_email: e.target.value })} /></label>
              <label className="block">Account number (optional)<input className={fieldClass} minLength={4} maxLength={34} pattern="[A-Za-z0-9]{4,34}" autoComplete="off" value={fields.account_number} onChange={e => update({ account_number: e.target.value })} /></label>
            </> : <>
              <label className="block">Card type<input className={fieldClass} required maxLength={150} placeholder="e.g. HDFC Millennia" value={fields.card_type} onChange={e => update({ card_type: e.target.value })} /></label>
              <div>
                <label className="block" htmlFor="financial-card-network">Card network</label>
                <Select id="financial-card-network" required value={fields.card_network} onChange={e => update({ card_network: e.target.value as AccountFields["card_network"] })}>
                  {Object.entries(networkLabels).map(([value, label]) => <option key={value} value={value} disabled={!value}>{label}</option>)}
                </Select>
              </div>
              <label className="block">Last four digits<input className={fieldClass} required maxLength={4} pattern="[0-9]{4}" inputMode="numeric" autoComplete="off" value={fields.last_four} onChange={e => update({ last_four: e.target.value })} /></label>
              <label className="block">Credit limit ({currency})<input className={fieldClass} type="number" required min={0} max={999999999999999} step="any" value={fields.credit_limit ?? ""} onChange={e => update({ credit_limit: e.target.value === "" ? null : Number(e.target.value) })} /></label>
              <label className="block">Expected statement day of month<input className={fieldClass} type="number" required min={1} max={31} step={1} value={fields.statement_day ?? ""} onChange={e => update({ statement_day: e.target.value === "" ? null : Number(e.target.value) })} /></label>
            </>}
            {error && <p role="alert">{error}</p>}
            <DialogActions>
              <DialogAction type="button" variant="secondary" disabled={pending} onClick={() => { setEditor(null); setError(""); }}>Cancel</DialogAction>
              <DialogAction disabled={pending}>{pending ? "Saving…" : "Save"}</DialogAction>
            </DialogActions>
          </form>
        </Dialog>
      )}
    </PulseCard>
  );
}
