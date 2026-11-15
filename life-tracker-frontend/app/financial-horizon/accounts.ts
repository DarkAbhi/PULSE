"use server";

import { apiFetch } from "@/app/lib/server-api";
import type { Account, AccountFields } from "./account-types";

const accountsURL = `${process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080"}/api/horizon/accounts`;

async function accountRequest<T>(path: string, init?: RequestInit) {
  try {
    const response = await apiFetch(`${accountsURL}${path}`, {
      ...init,
      cache: "no-store",
      signal: AbortSignal.timeout(10_000),
    });
    const body = await response.json();
    if (!response.ok) return { ok: false as const, error: body.error ?? "Unable to update accounts." };
    return { ok: true as const, data: body as T };
  } catch {
    return { ok: false as const, error: "Unable to reach the server. Please try again." };
  }
}

export async function listAccounts(includeArchived = false) {
  return accountRequest<{ accounts: Account[] }>(`?include_archived=${includeArchived}`);
}

export async function saveAccount(id: number | null, fields: AccountFields) {
  return accountRequest<Account>(id === null ? "" : `/${Number(id)}`, {
    method: id === null ? "POST" : "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(fields),
  });
}

export async function setAccountArchived(id: number, archived: boolean) {
  return accountRequest<Account>(`/${Number(id)}/${archived ? "archive" : "restore"}`, { method: "POST" });
}
