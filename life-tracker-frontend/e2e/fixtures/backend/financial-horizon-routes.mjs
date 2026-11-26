// Financial Horizon summary, transactions, saved items and accounts.
const accountsByUser = new Map();
const horizonCategories = [
  { id: 1, name: "Test category", allows_subcategories: true, parent_id: null },
  { id: 2, name: "Test detail", allows_subcategories: false, parent_id: 1 },
  { id: 3, name: "Test standalone", allows_subcategories: false, parent_id: null },
  { id: 4, name: "others", allows_subcategories: false, parent_id: 1 },
].map(category => ({ ...category, icon: "tag", color: "#64748b", is_default: true }));
const horizonTransactions = [
  { id: 1, name: "Test parent expense", category_id: 1, category_name: "Test category" },
  { id: 2, name: "Test child expense", category_id: 2, category_name: "Test category / Test detail", source: "statement", original_narration: "UPI/DR/104332516949/ICCL ZER/HDFC/zerodha/" },
  { id: 3, name: "Test standalone expense", category_id: 3, category_name: "Test standalone" },
].map(tx => ({ ...tx, type: "debit", amount: 10, transaction_date: "2026-01-02T10:00:00Z" }));
let horizonMutationIds = 100;
const horizonSaved = { budgets: [], deductions: [], subscriptions: [], purchases: [] };

/** Restores this domain's data for one session so every test starts from the same state. */
export function reset(token) {
  accountsByUser.delete(token);
  if (token === "horizon-mutations") for (const collection of Object.keys(horizonSaved)) horizonSaved[collection] = [];
}

/** Answers the request and returns true, or returns undefined to let the next domain try. */
export function route({ req, res, url, token, body, send }) {
  if (url.pathname === "/api/horizon") {
    const saved = token === "horizon-mutations" ? horizonSaved : { budgets: [], deductions: [], subscriptions: [] };
    return send(200, {
      base_amount: 0, currency: "INR", total_deductions: saved.deductions.reduce((sum, item) => sum + item.amount, 0), total_transactions: 0,
      remaining_amount: 0, committed_ratio: 0, total_budgets_allocated: 0, total_subscription_burn: 0,
      budgets: saved.budgets, deductions: saved.deductions, categories: token === "categories-test" ? horizonCategories : [],
      transactions: [], subscriptions: saved.subscriptions, projections: [],
    });
  }
  if (url.pathname === "/api/horizon/transactions") {
    const categoryId = Number(url.searchParams.get("category_id"));
    const transactions = token === "categories-test" ? horizonTransactions.filter(tx => {
      const category = horizonCategories.find(item => item.id === tx.category_id);
      return !categoryId || tx.category_id === categoryId || category.parent_id === categoryId;
    }) : [];
    return send(200, {
      transactions, page: 1, page_size: 10, total: transactions.length, total_pages: transactions.length ? 1 : 0,
    });
  }
  // Financial Horizon mutations for the "horizon-mutations" session, kept like a backend would so
  // revalidated reads return what was saved.
  if (token === "horizon-mutations") {
    const collections = { "horizon/budgets": "budgets", "horizon/deductions": "deductions", "horizon/subscriptions": "subscriptions", "next-month-purchases": "purchases" };
    const created = {
      budgets: () => ({ name: body.name, allocated_amount: body.allocated_amount, used_amount: 0, available_amount: body.allocated_amount, usage_percentage: 0 }),
      deductions: () => ({ name: body.name, category: body.category, amount: body.amount, due_day: body.due_day, budget_id: body.budget_id, is_active: true }),
      subscriptions: () => ({
        ...body, next_renewal_date: "2026-02-01", monthly_equivalent_amount: body.amount,
        linked_transaction_count: 0, total_spent: 0, created_at: "2026-01-01T00:00:00Z", updated_at: "2026-01-01T00:00:00Z",
      }),
      purchases: () => ({ name: body.name, price: body.price, url: body.url }),
    };
    const match = url.pathname.match(/^\/api\/(horizon\/budgets|horizon\/deductions|horizon\/subscriptions|next-month-purchases)(?:\/(\d+))?$/);
    const collection = match && collections[match[1]];
    if (collection && !match[2] && req.method === "POST") {
      const item = { id: ++horizonMutationIds, ...created[collection]() };
      horizonSaved[collection].unshift(item);
      return send(201, item);
    }
    if (collection && match[2] && req.method === "DELETE") {
      horizonSaved[collection] = horizonSaved[collection].filter(item => item.id !== Number(match[2]));
      return send(204);
    }
    if (url.pathname === "/api/next-month-purchases") return send(200, { items: horizonSaved.purchases, total: horizonSaved.purchases.length });
  }
  if (url.pathname === "/api/next-month-purchases") return send(200, { items: [], total: 0 });
  if (url.pathname === "/api/horizon/accounts") {
    const accounts = accountsByUser.get(token) ?? [{
      id: 1, display_name: "Test savings", institution_name: "Kotak", account_type: "bank_account",
      bank_account_subtype: "savings", last_four: "", archived_at: null, account_number: "", holder_name: "Test Person", holder_email: "test@example.com", card_type: "", card_network: "", credit_limit: null, statement_day: null,
    }];
    if (req.method === "GET") return send(200, { accounts });
    if (req.method === "POST") {
      const account = { ...body, id: accounts.length + 1, archived_at: null };
      accounts.push(account);
      accountsByUser.set(token, accounts);
      return send(201, account);
    }
  }
  const accountPath = url.pathname.match(/^\/api\/horizon\/accounts\/(\d+)(?:\/(archive|restore))?$/);
  if (accountPath) {
    const accounts = accountsByUser.get(token) ?? [];
    const account = accounts.find(item => item.id === Number(accountPath[1]));
    if (!account) return send(404, { error: "Account not found" });
    if (req.method === "PUT") Object.assign(account, body);
    else account.archived_at = accountPath[2] === "archive" ? new Date().toISOString() : null;
    return send(200, account);
  }
}
