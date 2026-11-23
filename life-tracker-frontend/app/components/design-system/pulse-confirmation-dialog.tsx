"use client";

import { useEffect, ReactNode, useId } from "react";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";

export interface PulseConfirmationDialogProps {
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

export default function PulseConfirmationDialog({
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
}: PulseConfirmationDialogProps) {
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
    <PulseDialog labelledBy={titleId} onBackdropClick={handleBackdropClick}>
        <PulseDialogHeader id={titleId}>{title}</PulseDialogHeader>
        <div className="mt-4 text-sm leading-6 text-muted-foreground">
          {description}
        </div>

        {error && (
          <p className="mt-3 text-sm text-destructive animate-pulse" role="alert">
            {error}
          </p>
        )}

        <PulseDialogActions>
          <PulseDialogAction
            className="w-full"
            variant="secondary"
            disabled={isLoading}
            onClick={onClose}
            type="button"
          >
            {cancelText}
          </PulseDialogAction>
          <PulseDialogAction
            className="w-full"
            variant={variant === "destructive" ? "destructive" : "primary"}
            disabled={isLoading}
            onClick={onConfirm}
            type="button"
          >
            {isLoading ? confirmLoadingText ?? "Loading…" : confirmText}
          </PulseDialogAction>
        </PulseDialogActions>
    </PulseDialog>
  );
}
