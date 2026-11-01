"use server";

import { apiFetch } from "@/app/lib/server-api";
import { revalidatePath } from "next/cache";

const apiBaseURL =
  process.env.INTERNAL_API_BASE_URL ??
  "http://localhost:8080";

export type TransactionPage = {
  transactions: unknown[];
  page: number;
  page_size: number;
  total: number;
  total_pages: number;
};

export async function getTransactionsPageAction(
  page: number,
  pageSize: number = 10,
  txType?: "debit" | "credit" | "all",
  search?: string
) {
  try {
    const params = new URLSearchParams({
      page: String(page),
      page_size: String(pageSize),
    });
    if (txType && txType !== "all") params.set("type", txType);
    if (search && search.trim()) params.set("search", search.trim());

    const response = await apiFetch(
      `${apiBaseURL}/api/horizon/transactions?${params.toString()}`,
      {  signal: AbortSignal.timeout(10_000) }
    );
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false as const, error: body.error ?? "Failed to load transactions." };
    }
    return { ok: true as const, data: body as TransactionPage };
  } catch {
    return { ok: false as const, error: "Unable to reach the server. Please try again." };
  }
}

export async function updateHorizonConfigAction(baseAmount: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/config`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ base_amount: baseAmount }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to update monthly base income." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, summary: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function addBudgetAction(name: string, allocatedAmount: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/budgets`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name, allocated_amount: allocatedAmount }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to create budget." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, budget: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function updateBudgetAction(id: number, name: string, allocatedAmount: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/budgets/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name, allocated_amount: allocatedAmount }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to update budget." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, budget: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function deleteBudgetAction(id: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/budgets/${id}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return { ok: false, error: "Failed to delete budget." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function addDeductionAction(
  name: string,
  category: string,
  amount: number,
  dueDay?: number | null,
  budgetId?: number | null
) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/deductions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name,
        category,
        amount,
        due_day: dueDay ?? null,
        budget_id: budgetId ?? null,
      }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to add fixed deduction." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, deduction: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function updateDeductionAction(
  id: number,
  name: string,
  category: string,
  amount: number,
  dueDay?: number | null,
  isActive: boolean = true,
  budgetId?: number | null
) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/deductions/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name,
        category,
        amount,
        due_day: dueDay ?? null,
        is_active: isActive,
        budget_id: budgetId ?? null,
      }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to update deduction." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, deduction: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function deleteDeductionAction(id: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/deductions/${id}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return { ok: false, error: "Failed to delete deduction." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

// Next Month Purchases Actions
export async function addNextMonthPurchaseHorizonAction(
  name: string,
  price: number,
  url: string | null
) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/next-month-purchases`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name, price, url }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "We couldn't save that item." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, item: body };
  } catch {
    return { ok: false, error: "We couldn't reach the server." };
  }
}

export async function deleteNextMonthPurchaseAction(itemID: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/next-month-purchases/${itemID}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return { ok: false, error: "We couldn't delete that purchase. Please try again." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}

export async function clearAllNextMonthPurchasesAction() {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/next-month-purchases`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return { ok: false, error: "We couldn't clear your planned purchases. Please try again." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true };
  } catch {
    return { ok: false, error: "We couldn't reach the server. Please try again." };
  }
}

// Category Actions
export async function getHorizonCategoriesAction() {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/categories`, {
      cache: "no-store",
    });
    const body = await response.json().catch(() => ([]));
    if (!response.ok) {
      return { ok: false, error: "Failed to fetch categories." };
    }
    return { ok: true, categories: body };
  } catch {
    return { ok: false, error: "Unable to reach the server." };
  }
}

export async function addHorizonCategoryAction(name: string, icon?: string, color?: string) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/categories`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name, icon, color }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to create category." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, category: body };
  } catch {
    return { ok: false, error: "Unable to reach the server." };
  }
}

// Transaction Actions
export async function addTransactionAction(
  name: string,
  amount: number,
  type?: "debit" | "credit",
  transactionDate?: string | null,
  categoryId?: number | null,
  budgetId?: number | null,
  subscriptionId?: number | null,
  notes?: string | null
) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/transactions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name,
        amount,
        type: type ?? "debit",
        transaction_date: transactionDate ?? null,
        category_id: categoryId ?? null,
        budget_id: budgetId ?? null,
        subscription_id: subscriptionId ?? null,
        notes: notes ?? null,
      }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to add transaction." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, transaction: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function bulkAddTransactionsAction(
  items: {
    name: string;
    amount: number;
    type?: "debit" | "credit";
    transactionDate?: string | null;
    categoryId?: number | null;
    budgetId?: number | null;
    subscriptionId?: number | null;
    notes?: string | null;
  }[]
) {
  try {
    const payload = items.map((item) => ({
      name: item.name,
      amount: item.amount,
      type: item.type ?? "debit",
      transaction_date: item.transactionDate ?? null,
      category_id: item.categoryId ?? null,
      budget_id: item.budgetId ?? null,
      subscription_id: item.subscriptionId ?? null,
      notes: item.notes ?? null,
    }));

    const response = await apiFetch(`${apiBaseURL}/api/horizon/transactions/bulk`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    const body = await response.json().catch(() => ([]));
    if (!response.ok) {
      return { ok: false, error: (body as any).error ?? "Failed to bulk add transactions." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, transactions: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function updateTransactionAction(
  id: number,
  name: string,
  amount: number,
  type?: "debit" | "credit",
  transactionDate?: string | null,
  categoryId?: number | null,
  budgetId?: number | null,
  subscriptionId?: number | null,
  notes?: string | null
) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/transactions/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        name,
        amount,
        type: type ?? "debit",
        transaction_date: transactionDate ?? null,
        category_id: categoryId ?? null,
        budget_id: budgetId ?? null,
        subscription_id: subscriptionId ?? null,
        notes: notes ?? null,
      }),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to update transaction." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, transaction: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function deleteTransactionAction(id: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/transactions/${id}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return { ok: false, error: "Failed to delete transaction." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

// Subscription Actions
export async function addSubscriptionAction(data: {
  name: string;
  amount: number;
  billing_cycle: string;
  billing_day?: number | null;
  renewal_date?: string | null;
  status?: string;
  category_id?: number | null;
  budget_id?: number | null;
  notes?: string | null;
}) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/subscriptions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to create subscription." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, subscription: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function updateSubscriptionAction(
  id: number,
  data: {
    name: string;
    amount: number;
    billing_cycle: string;
    billing_day?: number | null;
    renewal_date?: string | null;
    status?: string;
    category_id?: number | null;
    budget_id?: number | null;
    notes?: string | null;
  }
) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/subscriptions/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false, error: body.error ?? "Failed to update subscription." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true, subscription: body };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function deleteSubscriptionAction(id: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/subscriptions/${id}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      return { ok: false, error: "Failed to delete subscription." };
    }

    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true };
  } catch {
    return { ok: false, error: "Unable to reach the server. Please try again." };
  }
}

export async function linkSubscriptionTransactionAction(subscriptionId: number, transactionId: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/subscriptions/${subscriptionId}/transactions`, {
      method: "POST",
      headers: { "Content-Type": "application/json",  },
      body: JSON.stringify({ transaction_id: transactionId }),
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
      return { ok: false as const, error: body.error ?? "Failed to link transaction." };
    }
    revalidatePath("/dashboard");
    revalidatePath("/financial-horizon");
    return { ok: true as const, transaction: body };
  } catch {
    return { ok: false as const, error: "Unable to reach the server. Please try again." };
  }
}

export async function getSubscriptionTransactionsAction(subscriptionId: number) {
  try {
    const response = await apiFetch(`${apiBaseURL}/api/horizon/subscriptions/${subscriptionId}/transactions`, {
      cache: "no-store",
    });

    const body = await response.json().catch(() => ([]));
    if (!response.ok) {
      return { ok: false, error: "Failed to fetch subscription transactions." };
    }

    return { ok: true, transactions: body };
  } catch {
    return { ok: false, error: "Unable to reach the server." };
  }
}
