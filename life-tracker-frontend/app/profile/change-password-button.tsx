"use client";

import PulseButton from "@/app/components/design-system/pulse-button";

import { useState } from "react";
import { Lock } from "lucide-react";
import ChangePasswordDialog from "@/app/profile/change-password-dialog";
import { iconTileClass, labelClass, settingButtonClass, settingRowClass, settingValueClass } from "@/app/profile/profile-classes";

export default function ChangePasswordButton() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  return (
    <div className={settingRowClass}>
      <span className={iconTileClass} aria-hidden="true"><Lock /></span>
      <div>
        <p className={labelClass}>Password</p>
        <p className={settingValueClass} aria-hidden="true">••••••••••</p>
      </div>
      <PulseButton variant="chip" size="lg" shape="pill" labelStyle="strong" className={settingButtonClass}
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
