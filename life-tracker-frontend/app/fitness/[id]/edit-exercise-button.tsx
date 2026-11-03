"use client";

import { useRef, useState } from "react";
import { Pencil } from "lucide-react";
import Button from "@/app/components/design-system/button";
import AddExerciseForm from "./add-exercise-form";
import type { SavedExercise } from "./actions";

export default function EditExerciseButton({ visitID, exercise }: { visitID: string; exercise: SavedExercise }) {
  const [isEditing, setIsEditing] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);

  function closeEditor() {
    setIsEditing(false);
    triggerRef.current?.focus();
  }

  return (
    <div className="mt-3">
      <Button ref={triggerRef} type="button" variant="secondary" size="sm"
        aria-haspopup="dialog" aria-label={`Edit ${exercise.name}`}
        onClick={() => setIsEditing(true)}>
        <Pencil className="h-4 w-4" /> Edit exercise
      </Button>
      {isEditing && <AddExerciseForm visitID={visitID} exercise={exercise} onClose={closeEditor} />}
    </div>
  );
}
