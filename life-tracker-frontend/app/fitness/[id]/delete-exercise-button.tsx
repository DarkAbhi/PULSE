"use client";

import { useState, useTransition } from "react";
import { Trash2 } from "lucide-react";
import Button from "../../components/design-system/button";
import ConfirmationDialog from "../../components/design-system/confirmation-dialog";
import { deleteExercise } from "./actions";

export default function DeleteExerciseButton({ visitID, exerciseID, exerciseName }: {
  visitID: string;
  exerciseID: number;
  exerciseName: string;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function handleDelete() {
    setError("");
    startTransition(async () => {
      const result = await deleteExercise(visitID, exerciseID);
      if (result.ok) setIsOpen(false);
      else setError(result.error ?? "We couldn't delete this exercise. Please try again.");
    });
  }

  return (
    <>
      <Button type="button" variant="destructiveOutline" size="sm"
        aria-label={`Delete ${exerciseName}`}
        disabled={isPending}
        onClick={() => { setError(""); setIsOpen(true); }}
      >
        <Trash2 className="h-4 w-4" /> Delete exercise
      </Button>
      <ConfirmationDialog
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        onConfirm={handleDelete}
        title="Delete this exercise?"
        description={`This permanently removes “${exerciseName}” and all its sets from this workout.`}
        confirmText="Delete exercise"
        confirmLoadingText="Deleting…"
        isLoading={isPending}
        error={error}
        variant="destructive"
      />
    </>
  );
}
