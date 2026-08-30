"use client";

import { useEffect, ReactNode, useId } from "react";
import Dialog, { DialogAction, DialogActions } from "./dialog";

export interface ConfirmationDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void | Promise<void>;
  title: string;
  description: ReactNode;
  confirmText: string;
  confirmLoadingText?: string;
  isLoading?: boolean;
  error?: string;
  variant?: "destructive" | "positive" | "amber";
  cancelText?: string;
}

export default function ConfirmationDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmText,
  confirmLoadingText,
  isLoading = false,
  error,
  variant = "destructive",
  cancelText = "Cancel",
}: ConfirmationDialogProps) {
  const titleId = useId();

  // Escape key listener to close modal
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !isLoading) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose, isLoading]);

  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget && !isLoading) {
      onClose();
    }
  };

  return (
    <Dialog
      labelledBy={titleId}
      onBackdropClick={handleBackdropClick}
      panelClassName="scale-100 transition-all duration-300 ease-out"
    >
        <h2
          className="text-2xl font-bold tracking-tight text-foreground"
          id={titleId}
        >
          {title}
        </h2>
        <div className="mt-2 text-sm leading-6 text-muted-foreground">
          {description}
        </div>

        {error && (
          <p className="mt-3 text-sm text-destructive animate-pulse" role="alert">
            {error}
          </p>
        )}

        <DialogActions>
          <DialogAction
            className="w-full"
            variant="secondary"
            disabled={isLoading}
            onClick={onClose}
            type="button"
          >
            {cancelText}
          </DialogAction>
          <DialogAction
            className="w-full"
            variant={variant === "destructive" ? "destructive" : "primary"}
            disabled={isLoading}
            onClick={onConfirm}
            type="button"
          >
            {isLoading ? confirmLoadingText ?? "Loading…" : confirmText}
          </DialogAction>
        </DialogActions>
    </Dialog>
  );
}
