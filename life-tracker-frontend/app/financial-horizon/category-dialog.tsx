"use client";

import Button from "../components/design-system/button";

import { useState, useEffect, useId } from "react";
import { X, Tag } from "lucide-react";
import Dialog, { DialogAction, DialogActions } from "../components/design-system/dialog";

export interface CategoryDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: { name: string; color: string }) => Promise<void> | void;
  isPending?: boolean;
}

export default function CategoryDialog({
  isOpen,
  onClose,
  onSave,
  isPending = false,
}: CategoryDialogProps) {
  const titleId = useId();

  const [catName, setCatName] = useState("");
  const [catColor, setCatColor] = useState("#3b82f6");
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    if (!isOpen) return;
    setCatName("");
    setCatColor("#3b82f6");
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
      await onSave({ name: catName.trim(), color: catColor });
    } catch {
      setErrorMsg("Failed to save category.");
    }
  };

  return (
    <Dialog
      labelledBy={titleId}
      onBackdropClick={handleBackdropClick}
      panelClassName="scale-100 transition-all duration-200 sm:p-6"
    >
        <div className="flex items-center justify-between border-b border-border pb-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Tag className="h-5 w-5" />
            </div>
            <h2 className="text-xl font-bold text-foreground" id={titleId}>
              Add Custom Category
            </h2>
          </div>
          <Button variant="icon"
            onClick={onClose}
            disabled={isPending}
            aria-label="Close"
            title="Close dialog"
          >
            <X className="h-5 w-5" />
          </Button>
        </div>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {errorMsg && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-3 text-xs font-semibold text-destructive">
              {errorMsg}
            </div>
          )}

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Category Name</label>
            <input
              type="text"
              required
              placeholder="e.g. Pet Care, Education, Gaming"
              value={catName}
              onChange={(e) => setCatName(e.target.value)}
              className="w-full rounded-xl border border-border bg-background px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-muted-foreground">Badge Color</label>
            <div className="flex items-center gap-3">
              <input
                type="color"
                value={catColor}
                onChange={(e) => setCatColor(e.target.value)}
                className="h-10 w-16 cursor-pointer rounded-lg border border-border bg-background p-1"
              />
              <span className="text-sm font-mono text-muted-foreground">{catColor}</span>
            </div>
          </div>

          <DialogActions className="border-t border-border pt-4">
            <DialogAction
              type="button"
              variant="secondary"
              disabled={isPending}
              onClick={onClose}
            >
              Cancel
            </DialogAction>
            <DialogAction
              type="submit"
              disabled={isPending}
            >
              {isPending ? "Saving…" : "Save Category"}
            </DialogAction>
          </DialogActions>
        </form>
    </Dialog>
  );
}
