export type AccountFields = {
  display_name: string;
  institution_name: string;
  account_type: "bank_account" | "credit_card";
  bank_account_subtype: "" | "savings" | "current";
  account_number: string;
  holder_name: string;
  holder_email: string;
  card_type: string;
  card_network: "" | "visa" | "mastercard" | "american_express" | "diners_club" | "rupay";
  credit_limit: number | null;
  statement_day: number | null;
  last_four: string;
};

export type Account = AccountFields & {
  id: number;
  archived_at: string | null;
};
