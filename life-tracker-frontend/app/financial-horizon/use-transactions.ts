"use client";

import { useEffect, useEffectEvent, useRef, useState, useTransition } from "react";
import { getTransactionsPageAction } from "@/app/financial-horizon/actions";
import type { TransactionItem, TransactionPage } from "@/app/financial-horizon/horizon-types";

type TransactionType = "all" | "debit" | "credit";

export function useTransactions(initialPage: TransactionPage, setError: (message: string) => void) {
  const [page, setPage] = useState(initialPage);
  const [total, setTotal] = useState(initialPage.total);
  const [type, setType] = useState<TransactionType>("all");
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<number | null>(null);
  const [isPending, startTransition] = useTransition();
  const latestRequest = useRef(0);
  const previousFilters = useRef({ type, search, category });

  function updateTransactions(update: (items: TransactionItem[]) => TransactionItem[]) {
    setPage(previous => ({ ...previous, transactions: update(previous.transactions) }));
  }

  function loadPage(pageNumber: number, pageSize = page.page_size) {
    const request = ++latestRequest.current;
    setError("");
    startTransition(async () => {
      const result = await getTransactionsPageAction(pageNumber, pageSize, type, search, category);
      if (request !== latestRequest.current) return;
      if (result.ok) setPage(result.data);
      else setError(result.error);
    });
  }

  const loadFirstPage = useEffectEvent(() => loadPage(1));

  useEffect(() => {
    const previous = previousFilters.current;
    if (previous.type === type && previous.search === search && previous.category === category) return;

    latestRequest.current++;
    const timer = window.setTimeout(() => {
      previousFilters.current = { type, search, category };
      loadFirstPage();
    }, previous.search === search ? 0 : 250);

    return () => window.clearTimeout(timer);
  }, [type, search, category]);

  return { page, total, setTotal, type, setType, search, setSearch, category, setCategory, updateTransactions, loadPage, isPending };
}
