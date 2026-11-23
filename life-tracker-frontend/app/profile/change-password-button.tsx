"use client";

import Button from "@/app/components/design-system/button";

import { useState } from "react";
import { Lock } from "lucide-react";
import ChangePasswordDialog from "./change-password-dialog";
import styles from "./profile.module.css";

export default function ChangePasswordButton() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  return (
    <div className={styles.row}>
      <span className={styles.tile} aria-hidden="true"><Lock /></span>
      <div>
        <p className={styles.rowLabel}>Password</p>
        <p className={styles.rowValue} aria-hidden="true">••••••••••</p>
      </div>
      <Button variant="secondary" className={`${styles.pill} ${styles.secondary}`}
        onClick={() => setIsDialogOpen(true)}
        type="button"
      >
        Change password
      </Button>

      <ChangePasswordDialog
        isOpen={isDialogOpen}
        onClose={() => setIsDialogOpen(false)}
      />
    </div>
  );
}
