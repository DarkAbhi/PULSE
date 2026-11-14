interface StatementTransaction {
  date: string;
  description: string;
  amount: number;
  type: "debit" | "credit";
  suggestedCategory?: string;
  notes?: string;
}

export interface StatementExtractionResponse {
  data: {
    statementType: "credit_card" | "debit_card" | "bank_account" | "other";
    accountNumber: string;
    statementPeriod: string;
    currency: string;
    totalDebits: number;
    totalCredits: number;
    transactions: StatementTransaction[];
  };
  rawJson: string;
}

export async function extractStatement(file: File): Promise<StatementExtractionResponse> {
  if (file.size === 0 || file.size > 10 * 1024 * 1024) {
    throw new Error("Choose a PDF smaller than 10 MB.");
  }
  if ((await file.slice(0, 5).text()) !== "%PDF-") {
    throw new Error("The selected file is not a PDF.");
  }

  throw new Error("Statement extraction is temporarily unavailable.");
}
