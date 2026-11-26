"use client";

import CategoryOptions from "@/features/financial-horizon/components/category-options";

import { useEffect, useState, type FormEvent } from "react";
import PulseButton from "@/components/ui/pulse-button";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/components/ui/pulse-dialog";
import PulseSelect from "@/components/ui/pulse-select";
import type { CategoryItem } from "@/features/financial-horizon/types/horizon-types";
import type { Account } from "@/features/financial-horizon/accounts/account-types";
import { listAccounts } from "@/features/financial-horizon/accounts/account-actions";
import {
  cancelImport, commitImport, editImport, getImport, getImportRows, ImportError, uploadStatement,
  type ImportBatch, type ImportEdit, type ImportResult, type RowPage,
} from "@/features/financial-horizon/transactions/statement-import-api";

interface Props {
  categories: CategoryItem[];
  onClose: () => void;
  onManageAccounts: () => void;
  onImported: (result: ImportResult) => void;
}

const inputClass = "block min-w-0 w-full px-2 py-1.5 text-base sm:text-sm";
const previewColumns = "lg:grid-cols-[3.75rem_minmax(0,1fr)_8.5rem_8rem_6rem_9rem]";
const fieldLabel = "mb-1 block text-xs font-medium text-muted-foreground lg:sr-only";
const descriptionClass = `${inputClass} min-h-10 whitespace-pre-wrap [overflow-wrap:anywhere] leading-5`;

export default function StatementUploadDialog({ categories, onClose, onManageAccounts, onImported }: Props) {
  const [accounts, setAccounts] = useState<Account[] | null>(null);
  const [accountId, setAccountId] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [errorCode, setErrorCode] = useState("");
  const [batch, setBatch] = useState<ImportBatch | null>(null);
  const [page, setPage] = useState<RowPage | null>(null);
  const [changes, setChanges] = useState<Record<number, ImportEdit>>({});
  const [confirmed, setConfirmed] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);

  function loadAccounts() {
    setError("");
    void listAccounts().then(response => {
      if (response.ok) setAccounts(response.data.accounts);
      else setError(response.error);
    });
  }

  useEffect(loadAccounts, []);

  function attempt(op: () => Promise<void>) {
    setBusy(true);
    setError("");
    setErrorCode("");
    void op().catch(err => {
      setError(err instanceof Error ? err.message : "The import request failed. Try again.");
      setErrorCode(err instanceof ImportError ? err.code : "");
    }).finally(() => setBusy(false));
  }

  function upload(event: FormEvent) {
    event.preventDefault();
    if (!file || !accountId || busy) return;
    if (file.size > 10 * 1024 * 1024) {
      setError("Choose a PDF up to 10 MiB.");
      return;
    }
    setProgress(null);
    attempt(async () => {
      const parsed = await uploadStatement(file, Number(accountId), setProgress);
      setBatch(parsed);
      setPage(parsed);
      setFile(null);
      setConfirmed(!parsed.account_confirmation_required);
    });
  }

  function change(edit: ImportEdit) {
    setChanges(current => ({ ...current, [edit.id]: { ...current[edit.id], ...edit } }));
  }

  async function save(current: ImportBatch) {
    if (!Object.keys(changes).length && !(confirmed && current.account_confirmation_required)) return current;
    const updated = await editImport(current.id, current.revision, confirmed, Object.values(changes));
    setBatch(updated);
    setPage(currentPage => currentPage && ({
      ...currentPage, rows: currentPage.rows.map(row => ({ ...row, ...changes[row.id] })),
    }));
    setChanges({});
    return updated;
  }

  function turnPage(target: number) {
    if (!batch) return;
    attempt(async () => {
      const current = await save(batch);
      setPage(await getImportRows(current.id, target));
    });
  }

  function reload() {
    if (!batch) return;
    attempt(async () => {
      const current = await getImport(batch.id);
      const rows = await getImportRows(batch.id, page?.page ?? 1);
      setBatch(current);
      setPage(rows);
      setChanges({});
      setConfirmed(!current.account_confirmation_required);
    });
  }

  function confirm() {
    if (!batch || !confirmed) return;
    attempt(async () => {
      const updated = await save(batch);
      const saved = await commitImport(updated.id, updated.revision);
      setResult(saved);
      onImported(saved);
    });
  }

  function close() {
    if (busy) return;
    if (!batch || result || errorCode === "BATCH_EXPIRED") onClose();
    else attempt(async () => { await cancelImport(batch.id); onClose(); });
  }

  const account = accounts?.find(item => String(item.id) === accountId);
  return (
    <PulseDialog
      size={batch && !result ? "xl" : "sm"}
      panelClassName={batch && !result ? "max-w-6xl! p-4! sm:p-6!" : undefined}
      labelledBy="statement-import-title"
    >
      <div className="mb-4"><PulseDialogHeader id="statement-import-title">Import statement</PulseDialogHeader></div>
      {error && <p role="alert" className="my-3">{error}</p>}
      {result ? (
        <>
          <p role="status">{result.imported} imported · {result.duplicates_skipped} duplicates skipped · {result.excluded} excluded</p>
          <PulseDialogActions><PulseDialogAction type="button" onClick={onClose}>Done</PulseDialogAction></PulseDialogActions>
        </>
      ) : batch && page ? (
        <div className="space-y-4">
          <p role="status">Balances reconciled · {batch.counts.parsed} rows · {batch.counts.duplicates} duplicates</p>
          {batch.ocr_used && <p>Text was read from a scan. Check each row against your statement.</p>}
          {batch.counts.new === 0 && <p>No new transactions are selected for import.</p>}
          {batch.account_confirmation_required && (
            <label className="flex items-center gap-2">
              <input type="checkbox" checked={confirmed} disabled={busy} onChange={event => setConfirmed(event.target.checked)} />
              This statement belongs to {account?.display_name ?? "the selected account"}.
            </label>
          )}
          <div className={`hidden gap-3 text-sm font-semibold lg:grid ${previewColumns}`} aria-hidden="true">
            <span>Include</span>
            <span>Description</span>
            <span>Date</span>
            <span>Amount (${batch.currency})</span>
            <span>Type</span>
            <span>Category</span>
          </div>
          <ul aria-label={`Statement rows, page ${page.page}`} className="text-sm">
            {page.rows.map(original => {
              const row = { ...original, ...changes[original.id] };
              const duplicate = row.duplicate_status === "skipped";
              const disabled = busy || duplicate;
              return (
                <li key={row.id} className={`grid min-w-0 items-start gap-3 border-t border-divider py-3 min-[23.4375rem]:grid-cols-2 md:grid-cols-4 ${previewColumns}`}>
                  <div className="min-[23.4375rem]:col-span-full lg:col-span-1 lg:pt-2">
                    {duplicate ? <span className="text-xs font-medium text-muted-foreground">Duplicate</span> : (
                      <label className="inline-flex items-center gap-2">
                        <input aria-label={`Include row ${row.id}`} type="checkbox" disabled={disabled} checked={row.included} onChange={e => change({ id: row.id, included: e.target.checked })} />
                        <span className="lg:sr-only">Include in import</span>
                      </label>
                    )}
                  </div>
                  <label className="min-w-0 min-[23.4375rem]:col-span-full lg:col-span-1">
                    <span className={fieldLabel}>Description</span>
                    <span className="relative block">
                      {/* Matching text keeps the editor tall enough as the value and width change. */}
                      <span aria-hidden="true" className={`${descriptionClass} invisible`}>{row.description + "\u200B"}</span>
                      <textarea className={`${descriptionClass} absolute inset-0 h-full resize-none`} aria-label={`Description for row ${row.id}`} maxLength={255} disabled={disabled} value={row.description} onChange={e => change({ id: row.id, description: e.target.value })} />
                    </span>
                    {row.warnings?.includes("POSSIBLE_MANUAL_MATCH") && (
                      <span className="mt-1 block text-muted-foreground">Possible match in transaction history</span>
                    )}
                  </label>
                  <label className="min-w-0">
                    <span className={fieldLabel}>Date</span>
                    <input className={inputClass} aria-label={`Date for row ${row.id}`} type="date" disabled={disabled} value={row.date} onChange={e => change({ id: row.id, date: e.target.value })} />
                  </label>
                  <label className="min-w-0">
                    <span className={fieldLabel}>Amount (${batch.currency})</span>
                    <input className={`${inputClass} tabular-nums`} aria-label={`Amount for row ${row.id}`} inputMode="decimal" disabled={disabled} value={row.amount} onChange={e => change({ id: row.id, amount: e.target.value })} />
                  </label>
                  <label className="min-w-0">
                    <span className={fieldLabel}>Type</span>
                    <PulseSelect className={inputClass} aria-label={`Type for row ${row.id}`} value={row.type} disabled={disabled} onChange={e => change({ id: row.id, type: e.target.value as "debit" | "credit" })}><option value="debit">Debit</option><option value="credit">Credit</option></PulseSelect>
                  </label>
                  <label className="min-w-0">
                    <span className={fieldLabel}>Category</span>
                    <PulseSelect className={inputClass} aria-label={`Category for row ${row.id}`} value={row.category_id ?? ""} disabled={disabled} onChange={e => change({ id: row.id, category_id: e.target.value ? Number(e.target.value) : null })}>
                      <option value="">Uncategorized</option>
                      <CategoryOptions categories={categories} />
                    </PulseSelect>
                  </label>
                  {original.original_narration && (
                    <details className="min-w-0 col-span-full text-muted-foreground">
                      <summary className="w-fit cursor-pointer rounded py-2 focus-visible:outline-2 focus-visible:outline-ring" aria-label={`Original statement narration for row ${row.id}`}>Original statement narration</summary>
                      <p className="whitespace-pre-wrap [overflow-wrap:anywhere]">{original.original_narration}</p>
                    </details>
                  )}
                </li>
              );
            })}
          </ul>
          {page.total > page.page_size && (
            <nav className="flex items-center gap-2" aria-label="Preview pages">
              <PulseButton variant="secondary" disabled={busy || page.page <= 1} onClick={() => turnPage(page.page - 1)}>Previous</PulseButton>
              <span>Page {page.page} of {Math.ceil(page.total / page.page_size)}</span>
              <PulseButton variant="secondary" disabled={busy || page.page * page.page_size >= page.total} onClick={() => turnPage(page.page + 1)}>Next</PulseButton>
            </nav>
          )}
          {errorCode === "STALE_REVISION" && <PulseButton onClick={reload}>Reload preview</PulseButton>}
          <PulseDialogActions>
            <PulseDialogAction type="button" variant="secondary" disabled={busy} onClick={close}>Cancel import</PulseDialogAction>
            <PulseDialogAction type="button" disabled={busy || !confirmed || ["BATCH_EXPIRED", "STALE_REVISION"].includes(errorCode)} onClick={confirm}>{busy ? "Working…" : "Confirm import"}</PulseDialogAction>
          </PulseDialogActions>
        </div>
      ) : (
        <form className="space-y-4" onSubmit={upload}>
          {accounts === null ? (
            error ? <PulseButton type="button" variant="secondary" onClick={loadAccounts}>Retry</PulseButton> : <p role="status">Loading accounts…</p>
          ) : accounts.length === 0 ? (
            <PulseButton type="button" variant="secondary" onClick={onManageAccounts}>Add a financial account</PulseButton>
          ) : (
            <>
              <label className="block">Financial account
                <PulseSelect className="mt-1 block w-full px-3" required disabled={busy} value={accountId} onChange={e => setAccountId(e.target.value)}>
                  <option value="">Choose an account</option>
                  {accounts.map(item => <option key={item.id} value={item.id}>{item.display_name} · {item.institution_name}</option>)}
                </PulseSelect>
              </label>
              <label className="block">Statement PDF
                <input className="mt-1 block w-full" type="file" accept="application/pdf,.pdf" required disabled={busy} onChange={e => setFile(e.target.files?.[0] ?? null)} />
              </label>
            </>
          )}
          {busy && <p role="status">{progress === 1 ? "Extracting and checking balances…" : progress === null ? "Uploading…" : `Uploading… ${Math.round(progress * 100)}%`}</p>}
          <PulseDialogActions>
            <PulseDialogAction type="button" variant="secondary" disabled={busy} onClick={close}>Close</PulseDialogAction>
            <PulseDialogAction disabled={busy || !accountId || !file}>Preview statement</PulseDialogAction>
          </PulseDialogActions>
        </form>
      )}
    </PulseDialog>
  );
}
