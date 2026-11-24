"use client";

import { useRef, useState } from "react";
import { Pencil } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import AddExerciseForm from "@/app/fitness/[id]/add-exercise-form";
import type { SavedExercise } from "@/app/fitness/[id]/actions";

export default function EditExerciseButton({ visitID, exercise }: { visitID: string; exercise: SavedExercise }) {
  const [isEditing, setIsEditing] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);

  function closeEditor() {
    setIsEditing(false);
    triggerRef.current?.focus();
  }

  return (
    <>
      <PulseButton ref={triggerRef} type="button" variant="icon" size="lg"
        aria-haspopup="dialog" aria-label={`Edit ${exercise.name}`}
        onClick={() => setIsEditing(true)}>
        <Pencil size={19} aria-hidden="true" />
      </PulseButton>
      {isEditing && <AddExerciseForm visitID={visitID} exercise={exercise} onClose={closeEditor} />}
    </>
  );
}
