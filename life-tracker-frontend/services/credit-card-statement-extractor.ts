import { GoogleGenAI, Type } from "@google/genai";

export interface StatementTransaction {
  date: string;
  description: string;
  amount: number;
  type: "debit" | "credit";
  suggestedCategory?: string;
}

export interface StatementResult {
  statementType?: "credit_card" | "debit_card" | "bank_account" | "other";
  accountNumber?: string;
  statementPeriod?: string;
  totalDebits?: number;
  totalCredits?: number;
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

  // 2. Enforce JSON response schema
  const responseSchema = {
    type: Type.OBJECT,
    properties: {
      statementType: {
        type: Type.STRING,
        enum: ["credit_card", "debit_card", "bank_account", "other"],
      },
      accountNumber: { type: Type.STRING },
      statementPeriod: { type: Type.STRING },
      totalDebits: { type: Type.NUMBER },
      totalCredits: { type: Type.NUMBER },
      transactions: {
        type: Type.ARRAY,
        items: {
          type: Type.OBJECT,
          properties: {
            date: { type: Type.STRING },
            description: { type: Type.STRING },
            amount: { type: Type.NUMBER },
            type: { type: Type.STRING, enum: ["debit", "credit"] },
            suggestedCategory: { type: Type.STRING },
          },
          required: ["date", "description", "amount", "type"],
        },
      },
    },
    required: ["transactions"],
  };

  // 3. Request structured content generation from Gemini model
  const modelName = options?.model || "gemini-3.6-flash";
  const categoryInstruction =
    options?.availableCategories && options.availableCategories.length > 0
      ? `Assign each transaction a 'suggestedCategory' selected from this list of available categories: ${JSON.stringify(
          options.availableCategories
        )}. If you cannot judge or if none fits well, assign "Other".`
      : `Assign each transaction a 'suggestedCategory' based on what you think this transaction is. If you cannot judge, assign "Other".`;

  const prompt =
    "Extract all transaction details and account summary from this statement PDF file. " +
    "For each transaction, extract the date and time if present (in ISO 8601 format YYYY-MM-DDTHH:mm:ss). " +
    "If time is not specified in the statement, return only the date in YYYY-MM-DD format. " +
    "If both date and time are missing, leave the date field empty. " +
    categoryInstruction + " " +
    "Identify whether this is a credit card, debit card, or bank account statement. " +
    "Return pure valid JSON adhering strictly to the JSON schema provided.";

  const response = await ai.models.generateContent({
    model: modelName,
    contents: [pdfPart, prompt],
    config: {
      responseMimeType: "application/json",
      responseSchema: responseSchema,
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
