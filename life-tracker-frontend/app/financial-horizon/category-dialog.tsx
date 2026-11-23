"use client";

import PulseSelect from "@/app/components/design-system/pulse-select";
import type { CategoryItem } from "@/app/dashboard/financial-horizon-card";


import { useState, useEffect, useId } from "react";
import { Tag } from "lucide-react";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";

export interface CategoryDialogProps {
  isOpen: boolean;
  onClose: () => void;
  categories: CategoryItem[];
  onSave: (data: { name: string; color: string; parentId: number | null }) => Promise<void> | void;
  error?: string;
  isPending?: boolean;
}

export default function CategoryDialog({
  isOpen,
  onClose,
  categories,
  onSave,
  error,
  isPending = false,
}: CategoryDialogProps) {
  const titleId = useId();

  const [parentId, setParentId] = useState<number | null>(null);
  const [catName, setCatName] = useState("");
  const [catColor, setCatColor] = useState("#285c70");
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    if (!isOpen) return;
    setCatName("");
    setParentId(null);
    setCatColor("#285c70");
    setErrorMsg("");
  }, [isOpen]);

  // Escape key listener to close modal
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !isPending) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose, isPending]);

  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget && !isPending) {
      onClose();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg("");

    if (!catName.trim()) {
      setErrorMsg("Category name is required.");
      return;
    }

    try {
      await onSave({ name: catName.trim(), color: catColor, parentId });
    } catch {
      setErrorMsg("Failed to save category.");
    }
  };

  return (
    <PulseDialog
      labelledBy={titleId}
      onBackdropClick={handleBackdropClick}
    >
        <PulseDialogHeader id={titleId} icon={<Tag className="size-5" />} onClose={onClose} closeDisabled={isPending}>
          Add Custom Category
        </PulseDialogHeader>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {(errorMsg || error) && (
            <div role="alert" className="rounded-xl bg-destructive/10 p-3 text-xs font-semibold text-destructive">
              {errorMsg || error}
            </div>
          )}

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Category Name</label>
            <input
              type="text"
              required
              maxLength={100}
              value={catName}
              onChange={(e) => setCatName(e.target.value)}
              className="w-full px-3.5 py-2.5 text-sm text-foreground"
            />
          </div>

          <label className="block space-y-1 text-xs font-semibold text-muted-foreground">
            Parent Category
            <PulseSelect
              value={parentId ?? ""}
              onChange={(e) => setParentId(e.target.value ? Number(e.target.value) : null)}
              className="w-full px-3 py-2.5 text-sm text-foreground"
            >
              <option value="">None (Top-level category)</option>
              {categories.filter(c => c.parent_id == null && c.allows_subcategories).map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </PulseSelect>
          </label>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Badge Color</label>
            <div className="flex items-center gap-3">
              <input
                type="color"
                value={catColor}
                onChange={(e) => setCatColor(e.target.value)}
                className="h-10 w-16 cursor-pointer rounded-lg bg-secondary p-1"
              />
              <span className="text-sm text-muted-foreground">{catColor}</span>
            </div>
          </div>

          <PulseDialogActions>
            <PulseDialogAction
              type="button"
              variant="secondary"
              disabled={isPending}
              onClick={onClose}
            >
              Cancel
            </PulseDialogAction>
            <PulseDialogAction
              type="submit"
              disabled={isPending}
            >
              {isPending ? "Saving…" : "Save Category"}
            </PulseDialogAction>
          </PulseDialogActions>
        </form>
    </PulseDialog>
  );
}
