"use client";

import Button from "../components/design-system/button";

import { useState } from "react";
import { KeyRound } from "lucide-react";
import ChangePasswordDialog from "./change-password-dialog";

export default function ChangePasswordButton() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  return (
    <>
      <Button variant="secondary" size="md"
        onClick={() => setIsDialogOpen(true)}
        type="button"
      >
        <KeyRound className="h-4 w-4 text-primary" />
        <span>Change Password</span>
      </Button>

      <ChangePasswordDialog
        isOpen={isDialogOpen}
        onClose={() => setIsDialogOpen(false)}
      />
    </>
  );
}
