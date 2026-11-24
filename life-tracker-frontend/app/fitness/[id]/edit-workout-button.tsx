"use client";

import { useState } from "react";
import { Pencil } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import { saveWorkoutType } from "@/app/fitness/actions";
import WorkoutTypeDialog from "@/app/fitness/workout-type-dialog";
import type { WorkoutClassification } from "@/app/fitness/workout-types";

export default function EditWorkoutButton({ visitID, classification }: { visitID: number; classification: WorkoutClassification }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <PulseButton type="button" variant="onHero" aria-haspopup="dialog" onClick={() => setOpen(true)}
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
