"use client";

import { useRef, useState } from "react";
import { Pencil } from "lucide-react";
import { workoutIconClass } from "./workout-detail-classes";
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
    <>
      <Button ref={triggerRef} type="button" variant="icon" size="lg" className={workoutIconClass}
        aria-haspopup="dialog" aria-label={`Edit ${exercise.name}`}
        onClick={() => setIsEditing(true)}>
        <Pencil size={19} aria-hidden="true" />
      </Button>
      {isEditing && <AddExerciseForm visitID={visitID} exercise={exercise} onClose={closeEditor} />}
    </>
  );
}
