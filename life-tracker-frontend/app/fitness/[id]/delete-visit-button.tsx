"use client";

import Button from "@/app/components/design-system/button";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Ellipsis, Trash2 } from "lucide-react";
import ConfirmationDialog from "@/app/components/design-system/confirmation-dialog";
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
        router.replace("/fitness");
      }
    });
  }

  return (
    <>
      <details className="relative">
        <summary className="grid h-11 w-11 cursor-pointer list-none place-items-center rounded-lg text-muted-foreground hover:bg-secondary [&::-webkit-details-marker]:hidden">
          <Ellipsis aria-hidden="true" className="h-5 w-5" />
          <span className="sr-only">Workout actions</span>
        </summary>
        <div className="absolute right-0 z-10 mt-1 w-max rounded-xl bg-card p-1 shadow-lg">
          <Button variant="menuItem" size="lg"
            onClick={() => { setError(""); setIsConfirmingDelete(true); }}
            type="button"
          >
            <Trash2 aria-hidden="true" className="h-4 w-4" /> Delete workout
          </Button>
        </div>
      </details>

      <ConfirmationDialog
        isOpen={isConfirmingDelete}
        onClose={() => setIsConfirmingDelete(false)}
        onConfirm={handleDelete}
        title="Delete this workout?"
        description="This permanently removes the workout and every exercise and set saved with it."
        confirmText="Delete Workout"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        error={error}
        variant="destructive"
      />
    </>
  );
}
