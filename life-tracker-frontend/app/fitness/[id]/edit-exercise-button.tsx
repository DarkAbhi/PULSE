"use client";

import { useRef, useState } from "react";
import { Pencil } from "lucide-react";
import PulseIconButton from "@/app/components/design-system/pulse-icon-button";
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
      <PulseIconButton ref={triggerRef} type="button" size="lg"
        aria-haspopup="dialog" aria-label={`Edit ${exercise.name}`}
        onClick={() => setIsEditing(true)}>
        <Pencil size={19} aria-hidden="true" />
      </PulseIconButton>
      {isEditing && <AddExerciseForm visitID={visitID} exercise={exercise} onClose={closeEditor} />}
    </>
  );
}
