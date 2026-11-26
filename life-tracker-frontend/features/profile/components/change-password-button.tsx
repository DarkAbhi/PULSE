"use client";

import PulseButton from "@/components/ui/pulse-button";

import { useState } from "react";
import { Lock } from "lucide-react";
import ChangePasswordDialog from "@/features/profile/components/change-password-dialog";
import { iconTileClass, labelClass, settingButtonClass, settingRowClass, settingValueClass } from "@/features/profile/styles/profile-classes";

export default function ChangePasswordButton() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  return (
    <div className={settingRowClass}>
      <span className={iconTileClass} aria-hidden="true"><Lock /></span>
      <div>
        <p className={labelClass}>Password</p>
        <p className={settingValueClass} aria-hidden="true">••••••••••</p>
      </div>
      <PulseButton variant="secondary" size="lg" labelStyle="strong" className={settingButtonClass}
        onClick={() => setIsDialogOpen(true)}
        type="button"
      >
        Change password
      </PulseButton>

      <ChangePasswordDialog
        isOpen={isDialogOpen}
        onClose={() => setIsDialogOpen(false)}
      />
    </div>
  );
}
