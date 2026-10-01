"use client";

import Button from "../../components/design-system/button";

import { useState, useTransition } from "react";
import { Plus } from "lucide-react";
import { addExercise } from "./actions";

type ExerciseSet = {
  reps: string;
  weight: string;
};

function newSet(): ExerciseSet {
  return { reps: "", weight: "" };
}

interface AddExerciseFormProps {
  visitID: string;
}

export default function AddExerciseForm({ visitID }: AddExerciseFormProps) {
  const [exerciseName, setExerciseName] = useState("");
  const [sets, setSets] = useState<ExerciseSet[]>([newSet()]);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");

  function updateSet(index: number, field: keyof ExerciseSet, value: string) {
    setSets((currentSets) =>
      currentSets.map((set, setIndex) =>
        setIndex === index ? { ...set, [field]: value } : set
      )
    );
  }

  function handleSave(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    const payloadSets = sets.map((set) => ({
      reps: Number(set.reps),
      weight: set.weight === "" ? null : Number(set.weight),
    }));

    startTransition(async () => {
      const res = await addExercise(visitID, exerciseName, payloadSets);
      if (!res.ok) {
        setError(res.error ?? "We couldn't save that exercise.");
      } else {
        setExerciseName("");
        setSets([newSet()]);
      }
    });
  }

  return (
    <aside className="h-fit rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
      <h2 className="text-xl font-semibold text-foreground">Add an exercise</h2>
      <form className="mt-6 space-y-5" onSubmit={handleSave}>
        <div>
          <label
            className="mb-2 block text-sm font-medium text-muted-foreground"
            htmlFor="exercise-name"
          >
            Workout name
          </label>
          <input
            className="w-full rounded-lg border border-border bg-background text-foreground px-4 py-3 outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/15"
            id="exercise-name"
            maxLength={100}
            onChange={(event) => setExerciseName(event.target.value)}
            placeholder="e.g. Barbell squat"
            required
            value={exerciseName}
          />
        </div>

        <fieldset className="space-y-3">
          <legend className="text-sm font-medium text-muted-foreground">Sets</legend>
          {sets.map((set, index) => (
            <div
              className="grid grid-cols-[auto_1fr_1fr] items-end gap-2"
              key={index}
            >
              <span className="pb-3 text-sm font-semibold text-muted-foreground">
                {index + 1}
              </span>
              <label className="text-xs font-medium text-muted-foreground">
                Reps
                <input
                  className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
                  min="1"
                  onChange={(event) =>
                    updateSet(index, "reps", event.target.value)
                  }
                  required
                  type="number"
                  value={set.reps}
                />
              </label>
              <label className="text-xs font-medium text-muted-foreground">
                Weight (kg)
                <input
                  className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
                  min="0"
                  onChange={(event) =>
                    updateSet(index, "weight", event.target.value)
                  }
                  placeholder="Optional"
                  step="0.5"
                  type="number"
                  value={set.weight}
                />
              </label>
            </div>
          ))}
          <Button variant="tertiary" size="md"
            icon={<Plus className="h-4 w-4" />}
            onClick={() => setSets((currentSets) => [...currentSets, newSet()])}
            type="button"
          >
            Add another set
          </Button>
        </fieldset>

        {error && (
          <p className="text-sm text-destructive" role="alert">
            {error}
          </p>
        )}
        <Button variant="primary" size="lg"
          className="w-full"
          disabled={isPending}
          type="submit"
        >
          {isPending ? "Saving exercise…" : "Save exercise"}
        </Button>
      </form>
    </aside>
  );
}
