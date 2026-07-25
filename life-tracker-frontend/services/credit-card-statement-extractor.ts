import { GoogleGenAI, Type } from "@google/genai";

export interface StatementTransaction {
  date: string;
  description: string;
  amount: number;
  type: "debit" | "credit";
  suggestedCategory: string;
  notes?: string;
}

export interface StatementResult {
  statementType: "credit_card" | "debit_card" | "bank_account" | "other";
  accountNumber: string;
  statementPeriod: string;
  currency: string;
  totalDebits: number;
  totalCredits: number;
  transactions: StatementTransaction[];
}

export interface StatementExtractionResponse {
  data: StatementResult;
  rawJson: string;
}

export interface ExtractOptions {
  apiKey?: string;
  model?: string;
  availableCategories?: string[];
}

/**
 * Standardized JSON Schema definition for financial account statements.
 * Enforces a strict, predictable JSON structure across all Gemini API extractions.
 */
export const standardizedStatementSchema = {
  type: Type.OBJECT,
  description: "Standardized extraction schema for financial account statements",
  properties: {
    statementType: {
      type: Type.STRING,
      enum: ["credit_card", "debit_card", "bank_account", "other"],
      description: "Type of financial statement",
    },
    accountNumber: {
      type: Type.STRING,
      description: "Extracted masked or full account number, e.g. XXXX-1234 or N/A",
    },
    statementPeriod: {
      type: Type.STRING,
      description: "Date range of the statement period, e.g. 01 Jul 2026 - 31 Jul 2026 or N/A",
    },
    currency: {
      type: Type.STRING,
      description: "Currency symbol or 3-letter code e.g. INR, USD, EUR",
    },
    totalDebits: {
      type: Type.NUMBER,
      description: "Total sum of all debit transactions/charges in the statement",
    },
    totalCredits: {
      type: Type.NUMBER,
      description: "Total sum of all credit transactions/payments in the statement",
    },
    transactions: {
      type: Type.ARRAY,
      description: "Complete list of line-item transactions extracted from the statement",
      items: {
        type: Type.OBJECT,
        properties: {
          date: {
            type: Type.STRING,
            description: "Date (YYYY-MM-DD) or Date+Time (YYYY-MM-DDTHH:mm:ss) of transaction",
          },
          description: {
            type: Type.STRING,
            description: "Clean title/name of the merchant, payee, or transaction description",
          },
          amount: {
            type: Type.NUMBER,
            description: "Positive numerical amount of the transaction",
          },
          type: {
            type: Type.STRING,
            enum: ["debit", "credit"],
            description: "Transaction type: debit (expense/withdrawal) or credit (income/payment)",
          },
          suggestedCategory: {
            type: Type.STRING,
            description: "Matching category name from available categories list, or 'Other'",
          },
          notes: {
            type: Type.STRING,
            description: "Optional reference ID or additional transaction details",
          },
        },
        required: ["date", "description", "amount", "type", "suggestedCategory"],
      },
    },
  },
  required: [
    "statementType",
    "accountNumber",
    "statementPeriod",
    "currency",
    "totalDebits",
    "totalCredits",
    "transactions",
  ],
};

/**
 * Converts a browser File or Blob object to a base64 encoded string.
 */
async function fileToBase64(file: File | Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      // Strip Data-URL header (e.g. "data:application/pdf;base64,")
      const base64 = result.includes(",") ? result.split(",")[1] : result;
      resolve(base64);
    };
    reader.onerror = (error) => reject(error);
    reader.readAsDataURL(file);
  });
}

/**
 * Service to extract transactions and account summary from any financial statement PDF file 
 * (credit card, debit card, or bank account statement) using Google Gemini Gen AI.
 * 
 * @param file - The browser File or Blob object uploaded by the user.
 * @param options - Optional configuration including custom API key or Gemini model.
 * @returns Object containing parsed StatementResult and pure raw JSON string.
 */
export async function extractStatementData(
  file: File | Blob,
  options?: ExtractOptions
): Promise<StatementExtractionResponse> {
  const apiKey = options?.apiKey || process.env.NEXT_PUBLIC_GEMINI_API_KEY || process.env.GEMINI_API_KEY;

  if (!apiKey) {
    throw new Error(
      "Gemini API Key is required. Please set NEXT_PUBLIC_GEMINI_API_KEY environment variable or pass an apiKey in options."
    );
  }

  const ai = new GoogleGenAI({ apiKey });

  // 1. Convert browser File/Blob to base64 inlineData format for Gemini API
  const mimeType = file.type || "application/pdf";
  const base64Data = await fileToBase64(file);

  const pdfPart = {
    inlineData: {
      data: base64Data,
      mimeType: mimeType,
    },
  };

  // 2. Build system prompt instructing Gemini to strictly adhere to the standardized format
  const categoryInstruction =
    options?.availableCategories && options.availableCategories.length > 0
      ? `Assign each transaction a 'suggestedCategory' selected from this exact list of available categories: ${JSON.stringify(
          options.availableCategories
        )}. If you cannot judge or if none fits well, assign "Other".`
      : `Assign each transaction a 'suggestedCategory' based on what you think this transaction is. If you cannot judge, assign "Other".`;

  const prompt =
    "You are a financial statement parsing engine. Extract all transaction details and account summary from this PDF statement file.\n\n" +
    "STRICT STANDARDIZED FORMAT REQUIREMENTS:\n" +
    "1. 'statementType': Must be strictly one of ['credit_card', 'debit_card', 'bank_account', 'other'].\n" +
    "2. 'accountNumber': Extracted account/card number (e.g. 'XXXX-1234') or 'N/A'.\n" +
    "3. 'statementPeriod': Date range of statement (e.g. '01 Jul 2026 - 31 Jul 2026') or 'N/A'.\n" +
    "4. 'currency': Currency symbol or code (e.g. 'INR', 'USD') or 'INR'.\n" +
    "5. 'totalDebits': Total sum of all debits/charges as a positive number.\n" +
    "6. 'totalCredits': Total sum of all credits/payments as a positive number.\n" +
    "7. 'transactions': Array of individual line items. For each transaction:\n" +
    "   - 'date': ISO 8601 date (YYYY-MM-DD) or date-time (YYYY-MM-DDTHH:mm:ss). If date is missing, leave empty string.\n" +
    "   - 'description': Clean merchant or payee transaction description.\n" +
    "   - 'amount': Positive numerical transaction value.\n" +
    "   - 'type': Must be strictly 'debit' or 'credit'.\n" +
    "   - 'suggestedCategory': " + categoryInstruction + "\n" +
    "   - 'notes': Optional reference ID or additional transaction details.\n\n" +
    "Return pure valid JSON adhering strictly to the JSON schema provided.";

  // 3. Request structured content generation from Gemini model with responseSchema
  const modelName = options?.model || "gemini-3.6-flash";
  const response = await ai.models.generateContent({
    model: modelName,
    contents: [pdfPart, prompt],
    config: {
      responseMimeType: "application/json",
      responseSchema: standardizedStatementSchema,
    },
  });

  const rawJson = response.text || "";

  if (!rawJson) {
    throw new Error("No response content received from Gemini model.");
  }

  const parsedData = JSON.parse(rawJson) as StatementResult;

  return {
    data: parsedData,
    rawJson: rawJson,
  };
}

// Backward compatibility alias for credit card extractor
export type CreditCardStatementResult = StatementResult;
export async function extractCreditCardStatementData(
  file: File | Blob,
  options?: ExtractOptions
): Promise<CreditCardStatementResult> {
  const result = await extractStatementData(file, options);
  return result.data;
}
