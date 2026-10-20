"use server";

import { cookies } from "next/headers";
import { extractStatementData } from "../../services/credit-card-statement-extractor";

export async function extractStatementAction(formData: FormData, categories: string[]) {
  const cookieHeader = (await cookies()).toString();
  const apiBaseURL = process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080";
  const session = await fetch(`${apiBaseURL}/api/auth/session`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
    signal: AbortSignal.timeout(10_000),
  });
  if (!session.ok) {
    throw new Error("Sign in before importing a statement.");
  }

  const file = formData.get("statement");
  if (!(file instanceof File) || file.size === 0 || file.size > 10 * 1024 * 1024) {
    throw new Error("Choose a PDF smaller than 10 MB.");
  }
  if (Buffer.from(await file.slice(0, 5).arrayBuffer()).toString() !== "%PDF-") {
    throw new Error("The selected file is not a PDF.");
  }

  try {
    return await extractStatementData(file, {
      availableCategories: Array.isArray(categories)
        ? categories.slice(0, 100).filter((name) => typeof name === "string").map((name) => name.slice(0, 100))
        : [],
    });
  } catch (error) {
    const status = error && typeof error === "object" && "status" in error ? error.status : undefined;
    console.error("Statement extraction failed", { status });
    if (status === 503) {
      throw new Error("Gemini is busy. Please try extracting the statement again shortly.");
    }
    throw new Error("Could not extract this statement. Check the server configuration or try another PDF.");
  }
}
