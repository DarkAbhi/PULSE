"use client";

import { useState } from "react";
import { Pencil } from "lucide-react";
import Button from "../../components/design-system/button";
import AddExerciseForm from "./add-exercise-form";
import type { SavedExercise } from "./actions";

export default function EditExerciseButton({ visitID, exercise }: { visitID: string; exercise: SavedExercise }) {
  const [isEditing, setIsEditing] = useState(false);
  return (
    <div className="mt-3">
      {isEditing ? <AddExerciseForm visitID={visitID} exercise={exercise}
        onSaved={() => setIsEditing(false)} onCancel={() => setIsEditing(false)} /> :
        <Button type="button" variant="secondary" size="sm" aria-label={`Edit ${exercise.name}`}
          onClick={() => setIsEditing(true)}><Pencil className="h-4 w-4" /> Edit exercise</Button>}
    </div>
  );
}
