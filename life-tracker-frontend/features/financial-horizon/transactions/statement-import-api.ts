export type ImportRow = {
  id: number;
  date: string;
  description: string;
  original_narration?: string;
  amount: string;
  type: "debit" | "credit";
  category_id: number | null;
  included: boolean;
  warnings: string[];
  duplicate_status: "new" | "skipped";
};

export type ImportEdit = Partial<Pick<ImportRow, "date" | "description" | "amount" | "type" | "category_id" | "included">> & { id: number };

export type ImportBatch = {
  id: string;
  status: string;
  currency: string;
  revision: number;
  ocr_used: boolean;
  account_confirmation_required: boolean;
  expires_at: string;
  reconciliation: { status: string };
  counts: { parsed: number; new: number; duplicates: number; excluded: number };
};

export type RowPage = { rows: ImportRow[]; page: number; page_size: number; total: number };
export type ImportResult = { imported: number; duplicates_skipped: number; excluded: number };

const base = "/api/horizon/statement-imports";

export class ImportError extends Error {
  readonly code: string;

  constructor(message: string, code: string) {
    super(message);
    this.code = code;
  }
}

export function uploadStatement(file: File, accountId: number, onProgress: (progress: number | null) => void) {
  return new Promise<ImportBatch & RowPage>((resolve, reject) => {
    const form = new FormData();
    form.append("account_id", String(accountId));
    form.append("file", file, "statement.pdf");
    const request = new XMLHttpRequest();
    request.open("POST", base);
    request.responseType = "json";
    request.timeout = 185_000;
    request.upload.onprogress = event => onProgress(event.lengthComputable ? event.loaded / event.total : null);
    request.upload.onload = () => onProgress(1);
    request.onload = () => {
      if (request.status === 201 && request.response?.id) resolve(request.response);
      else reject(new ImportError(request.response?.error ?? "The statement could not be processed.", request.response?.code ?? ""));
    };
    request.onerror = request.ontimeout = () => reject(new Error("The upload was interrupted. Try uploading the same PDF again."));
    request.send(form);
  });
}

async function request<T>(path: string, method = "GET", input?: unknown): Promise<T> {
  const response = await fetch(`${base}/${path}`, {
    method,
    cache: "no-store",
    signal: AbortSignal.timeout(18_000),
    ...(input === undefined ? {} : { headers: { "Content-Type": "application/json" }, body: JSON.stringify(input) }),
  });
  if (response.status === 204) return undefined as T;
  const body = await response.json().catch(() => ({}));
  if (!response.ok) throw new ImportError(body.error ?? "The import request failed. Try again.", body.code ?? "");
  return body as T;
}

export const getImport = (id: string) => request<ImportBatch>(id);
export const getImportRows = (id: string, page: number) => request<RowPage>(`${id}/rows?page=${page}`);
export const editImport = (id: string, revision: number, confirmed: boolean, changes: ImportEdit[]) =>
  request<ImportBatch>(`${id}/rows`, "PATCH", { revision, account_confirmed: confirmed, changes });
export const commitImport = (id: string, revision: number) =>
  request<ImportResult>(`${id}/commit`, "POST", { revision });
export const cancelImport = (id: string) => request<void>(id, "DELETE");
