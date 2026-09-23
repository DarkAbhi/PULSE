"use client";

import Button from "../../components/design-system/button";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Trash2 } from "lucide-react";
import ConfirmationDialog from "../../components/design-system/confirmation-dialog";
import { deleteVisit } from "./actions";

interface DeleteVisitButtonProps {
  visitID: string;
}

export default function DeleteVisitButton({ visitID }: DeleteVisitButtonProps) {
  const router = useRouter();
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleDelete() {
    setError("");
    startTransition(async () => {
      const res = await deleteVisit(visitID);
      if (!res.ok) {
        setError(res.error ?? "We couldn't delete this gym visit. Please try again.");
      } else {
        setIsConfirmingDelete(false);
        router.replace("/gym-visits");
      }
    });
  }

  return (
    <>
      <Button variant="destructiveOutline" size="md"
        onClick={() => setIsConfirmingDelete(true)}
        type="button"
      >
        <Trash2 className="h-4 w-4" /> Delete visit
      </Button>

      <ConfirmationDialog
        isOpen={isConfirmingDelete}
        onClose={() => setIsConfirmingDelete(false)}
        onConfirm={handleDelete}
        title="Delete this gym visit?"
        description="This permanently removes the visit and every exercise and set saved with it."
        confirmText="Delete visit"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        error={error}
        variant="destructive"
      />
    </>
  );
}
