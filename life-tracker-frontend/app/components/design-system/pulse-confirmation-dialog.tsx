"use client";

import { ReactNode, useId } from "react";
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

  if (!isOpen) return null;

  return (
    <PulseDialog labelledBy={titleId} onDismiss={isLoading ? undefined : onClose}>
        <PulseDialogHeader id={titleId}>{title}</PulseDialogHeader>
        <div className="mt-4 text-sm leading-6 text-muted-foreground">
          {description}
        </div>

        {error && (
          <p className="mt-3 text-sm text-destructive" role="alert">
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
