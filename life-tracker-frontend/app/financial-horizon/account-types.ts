export type AccountFields = {
  display_name: string;
  institution_name: string;
  account_type: "bank_account" | "credit_card";
  bank_account_subtype: "" | "savings" | "current" | "other";
  currency: string;
  country_code: string;
  last_four: string;
};

export type Account = AccountFields & {
  id: number;
  archived_at: string | null;
};
