"use client";

import { useState } from "react";
import { KeyRound } from "lucide-react";
import ChangePasswordDialog from "./change-password-dialog";

export default function ChangePasswordButton() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  return (
    <>
      <button
        onClick={() => setIsDialogOpen(true)}
        type="button"
        className="flex items-center gap-2 rounded-xl border border-border bg-background px-4 py-2 text-sm font-semibold text-foreground shadow-xs transition hover:bg-muted focus:outline-none focus:ring-2 focus:ring-primary/20 cursor-pointer"
      >
        <KeyRound className="h-4 w-4 text-primary" />
        <span>Change Password</span>
      </button>

      <ChangePasswordDialog
        isOpen={isDialogOpen}
        onClose={() => setIsDialogOpen(false)}
      />
    </>
  );
}
