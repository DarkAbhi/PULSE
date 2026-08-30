"use client";

import { useState, useRef } from "react";
import {
  X,
  Upload,
  FileText,
  Sparkles,
  CheckCircle2,
  AlertCircle,
  Copy,
  Check,
  Code2,
  List,
  Plus,
  Loader2,
  Key,
} from "lucide-react";
import {
  extractStatementData,
  StatementExtractionResponse,
  StatementTransaction,
} from "../../../services/credit-card-statement-extractor";
import { CategoryItem } from "../../dashboard/financial-horizon-card";
import Dialog from "../../components/design-system/dialog";

interface StatementUploadDialogProps {
  isOpen: boolean;
  onClose: () => void;
  categories: CategoryItem[];
  onImportTransactions: (
    transactions: {
      name: string;
      amount: number;
      type?: "debit" | "credit";
      transactionDate: string;
      categoryId?: number | null;
      notes?: string | null;
    }[]
  ) => Promise<void>;
}

/**
 * Processes raw extracted transaction date:
 * - If date AND time are present, returns full ISO timestamp.
 * - If ONLY date is present, returns date formatted as YYYY-MM-DD.
 * - If BOTH date and time are missing/invalid, defaults to current date and time.
 */
export function processTransactionDate(rawDateStr?: string | null): string {
  if (!rawDateStr || !rawDateStr.trim()) {
    return new Date().toISOString();
  }

  const str = rawDateStr.trim();
  const parsed = new Date(str);

  if (isNaN(parsed.getTime())) {
    return new Date().toISOString();
  }

  // Check if string contains time components (e.g. "T", ":", "14:30", "02:30 PM")
  const hasTime = /[T\:\s]\d{1,2}:\d{2}|am|pm/i.test(str);

  if (hasTime) {
    return parsed.toISOString();
  }

  // Date only: format as YYYY-MM-DD
  const year = parsed.getFullYear();
  const month = String(parsed.getMonth() + 1).padStart(2, "0");
  const day = String(parsed.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export default function StatementUploadDialog({
  isOpen,
  onClose,
  categories,
  onImportTransactions,
}: StatementUploadDialogProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [extractionResult, setExtractionResult] = useState<StatementExtractionResponse | null>(null);
  const [activeView, setActiveView] = useState<"json" | "preview">("json");
  const [copied, setCopied] = useState(false);
  const [customApiKey, setCustomApiKey] = useState("");
  const [showApiKeyInput, setShowApiKeyInput] = useState(false);
  const [selectedTxIndexes, setSelectedTxIndexes] = useState<number[]>([]);
  const [selectedCategoryMap, setSelectedCategoryMap] = useState<Record<number, number | null>>({});
  const [isImporting, setIsImporting] = useState(false);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  if (!isOpen) return null;

  const handleFileSelect = (file: File) => {
    if (!file.name.toLowerCase().endsWith(".pdf") && file.type !== "application/pdf") {
      setError("Please select a PDF statement file.");
      return;
    }
    setSelectedFile(file);
    setError(null);
    setExtractionResult(null);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = () => {
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileSelect(e.dataTransfer.files[0]);
    }
  };

  const handleExtract = async () => {
    if (!selectedFile) return;

    setIsLoading(true);
    setError(null);

    try {
      const categoryNames = categories.map((c) => c.name);
      const res = await extractStatementData(selectedFile, {
        apiKey: customApiKey.trim() || undefined,
        availableCategories: categoryNames,
      });

      setExtractionResult(res);
      // Pre-select all extracted transactions for preview import
      if (res.data.transactions) {
        setSelectedTxIndexes(res.data.transactions.map((_, idx) => idx));

        // Find fallback 'Other' category ID if present in user categories
        const defaultOtherCategory = categories.find(
          (c) => c.name.toLowerCase() === "other"
        );
        const defaultOtherId = defaultOtherCategory ? defaultOtherCategory.id : null;

        // Auto-match categories by suggestedCategory or default to 'Other'
        const initialCategoryMap: Record<number, number | null> = {};
        res.data.transactions.forEach((tx, idx) => {
          if (tx.suggestedCategory) {
            const match = categories.find(
              (c) => c.name.toLowerCase() === tx.suggestedCategory?.toLowerCase()
            );
            initialCategoryMap[idx] = match ? match.id : defaultOtherId;
          } else {
            initialCategoryMap[idx] = defaultOtherId;
          }
        });
        setSelectedCategoryMap(initialCategoryMap);
      }
    } catch (err: any) {
      setError(err?.message || "Failed to extract statement data. Please check your Gemini API key or file.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleCopyJson = () => {
    if (!extractionResult?.rawJson) return;
    navigator.clipboard.writeText(extractionResult.rawJson);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const toggleSelectTx = (index: number) => {
    setSelectedTxIndexes((prev) =>
      prev.includes(index) ? prev.filter((i) => i !== index) : [...prev, index]
    );
  };

  const toggleSelectAll = () => {
    if (!extractionResult?.data.transactions) return;
    if (selectedTxIndexes.length === extractionResult.data.transactions.length) {
      setSelectedTxIndexes([]);
    } else {
      setSelectedTxIndexes(extractionResult.data.transactions.map((_, idx) => idx));
    }
  };

  const handleImport = async () => {
    if (!extractionResult?.data.transactions || selectedTxIndexes.length === 0) return;

    setIsImporting(true);
    try {
      const itemsToImport = selectedTxIndexes.map((idx) => {
        const tx = extractionResult.data.transactions[idx];
        const formattedTxDate = processTransactionDate(tx.date);

        return {
          name: tx.description || "Statement Transaction",
          amount: Math.abs(tx.amount),
          type: (tx.type && tx.type.toLowerCase() === "credit" ? "credit" : "debit") as "debit" | "credit",
          transactionDate: formattedTxDate,
          categoryId: selectedCategoryMap[idx] ?? null,
          notes: `Imported from statement${
            extractionResult.data.accountNumber ? ` (${extractionResult.data.accountNumber})` : ""
          }`,
        };
      });

      await onImportTransactions(itemsToImport);
      onClose();
    } catch (err: any) {
      setError(err?.message || "Failed to import transactions.");
    } finally {
      setIsImporting(false);
    }
  };

  return (
    <Dialog panelClassName="relative rounded-3xl p-0 overflow-hidden" size="xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border px-6 py-4 bg-secondary/30">
          <div className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary font-bold">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-foreground">Import Statement PDF</h3>
              <p className="text-xs text-muted-foreground">
                Extract transactions from credit card, debit card, or bank statements via Gemini Gen AI
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="rounded-full p-1.5 text-muted-foreground hover:bg-secondary hover:text-foreground transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 space-y-6 max-h-[75vh] overflow-y-auto scrollbar-thin">
          {/* API Key Toggle/Input */}
          <div className="rounded-xl border border-border/80 bg-background/60 p-3 text-xs space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-muted-foreground flex items-center gap-1.5 font-medium">
                <Key className="h-3.5 w-3.5 text-primary" /> Gemini API Key Config
              </span>
              <button
                type="button"
                onClick={() => setShowApiKeyInput(!showApiKeyInput)}
                className="text-primary hover:underline font-semibold text-[11px]"
              >
                {showApiKeyInput ? "Hide Custom Key" : "Set Custom API Key"}
              </button>
            </div>
            {showApiKeyInput && (
              <input
                type="password"
                placeholder="Enter custom GEMINI_API_KEY (leave blank to use environment variable)"
                value={customApiKey}
                onChange={(e) => setCustomApiKey(e.target.value)}
                className="w-full rounded-lg border border-border bg-card px-3 py-1.5 text-xs text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
              />
            )}
          </div>

          {/* File Upload Zone */}
          {!extractionResult && (
            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`flex flex-col items-center justify-center rounded-2xl border-2 border-dashed p-8 text-center cursor-pointer transition ${
                isDragOver
                  ? "border-primary bg-primary/5"
                  : selectedFile
                  ? "border-emerald-500/50 bg-emerald-500/5"
                  : "border-border/80 bg-secondary/20 hover:border-primary/50 hover:bg-secondary/40"
              }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,application/pdf"
                className="hidden"
                onChange={(e) => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
              />
              {selectedFile ? (
                <div className="flex flex-col items-center gap-2">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400">
                    <FileText className="h-6 w-6" />
                  </div>
                  <h4 className="font-bold text-foreground text-sm">{selectedFile.name}</h4>
                  <p className="text-xs text-muted-foreground">
                    {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB PDF Document
                  </p>
                  <span className="mt-2 text-xs font-semibold text-primary hover:underline">
                    Click to change file
                  </span>
                </div>
              ) : (
                <div className="flex flex-col items-center gap-2">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                    <Upload className="h-6 w-6" />
                  </div>
                  <h4 className="font-bold text-foreground text-sm">
                    Drag and drop your statement PDF here
                  </h4>
                  <p className="text-xs text-muted-foreground">
                    Supports monthly Credit Card, Debit Card, or Bank Account statements
                  </p>
                  <button
                    type="button"
                    className="mt-2 inline-flex items-center gap-1.5 rounded-xl border border-border bg-card px-3.5 py-1.5 text-xs font-semibold text-foreground shadow-xs hover:bg-secondary"
                  >
                    Browse Computer
                  </button>
                </div>
              )}
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="flex items-center gap-2.5 rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-xs text-destructive">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Extracting Loading Indicator */}
          {isLoading && (
            <div className="flex flex-col items-center justify-center py-10 space-y-3">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
              <p className="text-sm font-semibold text-foreground">
                Analyzing statement with Google Gemini Gen AI...
              </p>
              <p className="text-xs text-muted-foreground">
                Extracting transactions and account summary into structured JSON
              </p>
            </div>
          )}

          {/* Extraction Results View */}
          {extractionResult && (
            <div className="space-y-4">
              {/* Header Tabs: Pure JSON vs Transaction List */}
              <div className="flex items-center justify-between border-b border-border pb-3">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setActiveView("json")}
                    className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition ${
                      activeView === "json"
                        ? "bg-primary text-primary-foreground shadow-xs"
                        : "bg-secondary/60 text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    <Code2 className="h-3.5 w-3.5" /> Pure JSON Output
                  </button>
                  <button
                    onClick={() => setActiveView("preview")}
                    className={`inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition ${
                      activeView === "preview"
                        ? "bg-primary text-primary-foreground shadow-xs"
                        : "bg-secondary/60 text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    <List className="h-3.5 w-3.5" /> Transactions (
                    {extractionResult.data.transactions?.length || 0})
                  </button>
                </div>

                {activeView === "json" ? (
                  <button
                    onClick={handleCopyJson}
                    className="inline-flex items-center gap-1.5 rounded-xl border border-border bg-card px-3 py-1.5 text-xs font-semibold text-foreground transition hover:bg-secondary"
                  >
                    {copied ? (
                      <>
                        <Check className="h-3.5 w-3.5 text-emerald-500" /> Copied!
                      </>
                    ) : (
                      <>
                        <Copy className="h-3.5 w-3.5" /> Copy JSON
                      </>
                    )}
                  </button>
                ) : (
                  <div className="flex items-center gap-2">
                    <button
                      onClick={toggleSelectAll}
                      className="text-xs text-primary font-semibold hover:underline"
                    >
                      {selectedTxIndexes.length === extractionResult.data.transactions.length
                        ? "Deselect All"
                        : "Select All"}
                    </button>
                  </div>
                )}
              </div>

              {/* Statement Overview Badge */}
              <div className="flex items-center gap-3 flex-wrap text-xs rounded-xl border border-border bg-secondary/20 p-3">
                {extractionResult.data.statementType && (
                  <span className="rounded-full bg-primary/10 border border-primary/20 px-2.5 py-0.5 font-semibold text-primary capitalize">
                    {extractionResult.data.statementType.replace("_", " ")}
                  </span>
                )}
                {extractionResult.data.accountNumber && (
                  <span className="text-muted-foreground">
                    Account: <strong className="text-foreground">{extractionResult.data.accountNumber}</strong>
                  </span>
                )}
                {extractionResult.data.totalDebits !== undefined && (
                  <span className="text-muted-foreground">
                    Total Debits:{" "}
                    <strong className="text-rose-500">
                      ₹{extractionResult.data.totalDebits.toLocaleString("en-IN")}
                    </strong>
                  </span>
                )}
                {extractionResult.data.totalCredits !== undefined && (
                  <span className="text-muted-foreground">
                    Total Credits:{" "}
                    <strong className="text-emerald-500">
                      ₹{extractionResult.data.totalCredits.toLocaleString("en-IN")}
                    </strong>
                  </span>
                )}
              </div>

              {/* Pure JSON Code View */}
              {activeView === "json" && (
                <div className="relative rounded-2xl border border-border bg-zinc-950 p-4 font-mono text-xs text-zinc-100 overflow-x-auto max-h-96 leading-relaxed">
                  <pre>{JSON.stringify(JSON.parse(extractionResult.rawJson), null, 2)}</pre>
                </div>
              )}

              {/* Transaction List Preview & Category Mapping View */}
              {activeView === "preview" && (
                <div className="space-y-2 max-h-96 overflow-y-auto scrollbar-thin pr-1">
                  {extractionResult.data.transactions.map((tx, idx) => {
                    const isSelected = selectedTxIndexes.includes(idx);
                    return (
                      <div
                        key={idx}
                        className={`flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-xl border p-3 text-xs transition ${
                          isSelected
                            ? "border-primary/50 bg-primary/5"
                            : "border-border bg-card opacity-60"
                        }`}
                      >
                        <div className="flex items-start gap-3 min-w-0">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => toggleSelectTx(idx)}
                            className="mt-0.5 rounded border-border text-primary focus:ring-primary h-4 w-4 shrink-0"
                          />
                          <div className="min-w-0">
                            <div className="font-semibold text-foreground truncate text-sm">
                              {tx.description}
                            </div>
                            <div className="text-muted-foreground text-[11px] flex items-center gap-2 mt-0.5">
                              <span>{tx.date}</span>
                              <span className="capitalize font-medium text-secondary-foreground">
                                • {tx.type}
                              </span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center gap-3 shrink-0 justify-between sm:justify-end border-t sm:border-t-0 pt-2 sm:pt-0 border-border/40">
                          {/* Category Selector */}
                          <select
                            value={selectedCategoryMap[idx] ?? ""}
                            onChange={(e) =>
                              setSelectedCategoryMap((prev) => ({
                                ...prev,
                                [idx]: e.target.value ? Number(e.target.value) : null,
                              }))
                            }
                            className="rounded-lg border border-border bg-background px-2 py-1 text-xs text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
                          >
                            <option value="">Uncategorized</option>
                            {categories.map((cat) => (
                              <option key={cat.id} value={cat.id}>
                                {cat.name}
                              </option>
                            ))}
                          </select>

                          <span
                            className={`font-bold text-sm ${
                              tx.type === "credit" ? "text-emerald-500" : "text-foreground"
                            }`}
                          >
                            ₹{tx.amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-between border-t border-border px-6 py-4 bg-secondary/20">
          {extractionResult ? (
            <button
              onClick={() => setExtractionResult(null)}
              className="text-xs font-semibold text-muted-foreground hover:text-foreground transition"
            >
              Upload Another Statement
            </button>
          ) : (
            <button
              onClick={onClose}
              className="rounded-xl border border-border bg-card px-4 py-2 text-xs font-semibold text-foreground hover:bg-secondary transition"
            >
              Cancel
            </button>
          )}

          {!extractionResult ? (
            <button
              onClick={handleExtract}
              disabled={!selectedFile || isLoading}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2 text-xs font-semibold text-primary-foreground shadow transition hover:opacity-90 disabled:opacity-50"
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" /> Extracting...
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4" /> Extract Transactions
                </>
              )}
            </button>
          ) : (
            <button
              onClick={handleImport}
              disabled={selectedTxIndexes.length === 0 || isImporting}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2 text-xs font-semibold text-primary-foreground shadow transition hover:opacity-90 disabled:opacity-50"
            >
              {isImporting ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" /> Importing...
                </>
              ) : (
                <>
                  <Plus className="h-4 w-4" /> Import {selectedTxIndexes.length} Transactions
                </>
              )}
            </button>
          )}
        </div>
    </Dialog>
  );
}
