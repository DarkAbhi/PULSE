"use client";

import Button from "../../components/design-system/button";
import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import ConfirmationDialog from "../../components/design-system/confirmation-dialog";
import { deleteVehicle } from "./actions";

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
        variant="destructiveOutline"
        size="md"
        onClick={() => setIsConfirmingDelete(true)}
        type="button"
      >
        <Trash2 className="h-4 w-4" /> Delete vehicle
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
