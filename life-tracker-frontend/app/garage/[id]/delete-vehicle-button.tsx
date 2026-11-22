"use client";

import Button from "@/app/components/design-system/button";
import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
import { deleteVehicle } from "./actions";
import styles from "./vehicle-detail.module.css";

interface DeleteVehicleButtonProps {
  vehicleId: string;
  vehicleName: string;
}

export default function DeleteVehicleButton({
  vehicleId,
  vehicleName,
}: DeleteVehicleButtonProps) {
  const router = useRouter();
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleDelete() {
    setError("");
    startTransition(async () => {
      const res = await deleteVehicle(vehicleId);
      if (!res.ok) {
        setError(res.error ?? "We couldn't delete this vehicle. Please try again.");
      } else {
        setIsConfirmingDelete(false);
        router.replace("/garage");
      }
    });
  }

  return (
    <>
      <Button
        variant="destructive"
        size="lg"
        className={`${styles.button} ${styles.dangerButton}`}
        aria-haspopup="dialog"
        onClick={() => setIsConfirmingDelete(true)}
        type="button"
      >
        <Trash2 className="size-4" aria-hidden="true" /> Delete vehicle
      </Button>

      <ConfirmationDialog
        isOpen={isConfirmingDelete}
        onClose={() => setIsConfirmingDelete(false)}
        onConfirm={handleDelete}
        title="Delete this vehicle?"
        description={`This permanently removes ${vehicleName} and all its fuel, air fill, and maintenance history.`}
        confirmText="Delete vehicle"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        error={error}
        variant="destructive"
      />
    </>
  );
}
