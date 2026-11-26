"use client";

import { useState } from "react";
import { Pencil } from "lucide-react";
import PulseButton from "@/components/ui/pulse-button";
import { saveWorkoutType } from "@/features/fitness/api/fitness-actions";
import WorkoutTypeDialog from "@/features/fitness/components/workout-type-dialog";
import type { WorkoutClassification } from "@/features/fitness/types/workout-types";

export default function EditWorkoutButton({ visitID, classification }: { visitID: number; classification: WorkoutClassification }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <PulseButton type="button" variant="secondary" className="[--btn-cancel-bg:white] [--btn-cancel-text:var(--on-hero-action)] [--btn-cancel-hover:var(--on-hero-hover)]" aria-haspopup="dialog" onClick={() => setOpen(true)}
        icon={<Pencil size={16} aria-hidden="true" />}>
        Edit workout
      </PulseButton>
      {open && (
        <WorkoutTypeDialog
          title="Edit workout"
          initialType={classification.workout_type}
          initialFocus={classification.workout_focus}
          onClose={() => setOpen(false)}
          onSave={(type, focus) => saveWorkoutType(String(visitID), type, focus)}
        />
      )}
    </>
  );
}
