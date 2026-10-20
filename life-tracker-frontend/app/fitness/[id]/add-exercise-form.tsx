"use client";

import Button from "../../components/design-system/button";

import { useEffect, useId, useState, useTransition } from "react";
import { Plus } from "lucide-react";
import { addExercise, updateExercise, searchExercises, type CatalogSearch, type SavedExercise } from "./actions";

type ExerciseSet = {
  reps: string;
  weight: string;
};

function newSet(): ExerciseSet {
  return { reps: "", weight: "" };
}

interface AddExerciseFormProps {
  visitID: string;
  exercise?: SavedExercise;
  onSaved?: () => void;
  onCancel?: () => void;
}

export default function AddExerciseForm({ visitID, exercise, onSaved, onCancel }: AddExerciseFormProps) {
  const fieldID = useId();
  const [exerciseName, setExerciseName] = useState(exercise?.name ?? "");
  const [searchName, setSearchName] = useState(exercise?.exercise_catalog_id ? "" : exercise?.name ?? "");
  const [sets, setSets] = useState<ExerciseSet[]>(exercise?.sets.map((set) => ({
    reps: String(set.reps), weight: set.weight === null ? "" : String(set.weight),
  })) ?? [newSet()]);
  const [isPending, startTransition] = useTransition();
  const [error, setError] = useState("");
  const [catalog, setCatalog] = useState<CatalogSearch | null>(exercise?.catalog_exercise ? {
    query: exercise.name, match_type: "exact", exercise_catalog_id: exercise.exercise_catalog_id,
    candidates: [{ ...exercise.catalog_exercise, match_type: "exact" }],
  } : null);
  const [catalogID, setCatalogID] = useState<string | null>(exercise?.exercise_catalog_id ?? null);
  const [rememberAlias, setRememberAlias] = useState(false);
  const [searching, setSearching] = useState(Boolean(exercise?.name && !exercise.exercise_catalog_id));
  const [searchError, setSearchError] = useState("");

  useEffect(() => {
    if (!searchName.trim()) return;
    let cancelled = false;
    const timer = setTimeout(async () => {
      const response = await searchExercises(searchName);
      if (cancelled) return;
      setCatalog(response.result ?? null);
      setCatalogID(response.result?.exercise_catalog_id ?? null);
      setSearchError(response.error ?? "");
      setSearching(false);
    }, 300);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [searchName]);

  function changeName(value: string, resetMatch = false) {
    setExerciseName(value);
    setRememberAlias(false);
    if (!resetMatch && catalogID !== null && searchName === "") return;
    setSearchName(value);
    setCatalog(null);
    setCatalogID(null);
    setSearchError("");
    setSearching(Boolean(value.trim()));
  }

  function selectMatch(id: string, name: string) {
    setExerciseName(name);
    setSearchName("");
    setCatalogID(id);
    setRememberAlias(false);
    setSearching(false);
  }

  const selected = catalog?.candidates.find((exercise) => exercise.id === catalogID);

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
      const res = exercise
        ? await updateExercise(visitID, exercise.id, exerciseName, payloadSets, catalogID, rememberAlias)
        : await addExercise(visitID, exerciseName, payloadSets, catalogID, rememberAlias);
      if (!res.ok) {
        setError(res.error ?? "We couldn't save that exercise.");
      } else {
        if (exercise) { onSaved?.(); return; }
        changeName("", true);
        setSets([newSet()]);
      }
    });
  }

  return (
    <aside className="h-fit rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
      <h2 className="text-xl font-semibold text-foreground">{exercise ? "Edit exercise" : "Add an exercise"}</h2>
      <form className="mt-6 space-y-5" onSubmit={handleSave}>
        <div>
          <label
            className="mb-2 block text-sm font-medium text-muted-foreground"
            htmlFor={fieldID}
          >
            Exercise name
          </label>
          <input
            className="w-full rounded-lg border border-border bg-background text-foreground px-4 py-3 outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/15"
            id={fieldID}
            maxLength={100}
            onChange={(event) => changeName(event.target.value)}
            placeholder="e.g. Barbell squat"
            required
            disabled={isPending}
            value={exerciseName}
          />
        </div>

        <div aria-live="polite" className="space-y-3 text-sm">
          {searching && <p className="text-muted-foreground">Finding exercises…</p>}
          {searchError && <p className="text-muted-foreground">{searchError}</p>}
          {catalog && <fieldset className="space-y-2">
            <legend className="mb-2 font-medium">Match your exercise</legend>
            <p className="mb-3 text-muted-foreground">
              {catalog.match_type === "exact" || catalog.match_type === "alias"
                ? "Matched your name. Check the equipment and variation."
                : "Choose the right equipment and variation, or keep your own name."}
            </p>
            {catalog.candidates.map((exercise) => <label key={exercise.id} className="flex cursor-pointer items-start gap-2 rounded-lg border border-border p-3">
              <input type="radio" name={`catalog-${fieldID}`} checked={catalogID === exercise.id}
                disabled={isPending} onChange={() => selectMatch(exercise.id, exercise.name)}
                onClick={() => selectMatch(exercise.id, exercise.name)} />
              <span>{exercise.name}<span className="block text-xs text-muted-foreground">
                {exercise.equipment ?? "Equipment unspecified"} · {exercise.data.primaryMuscles.join(", ")}
              </span></span>
            </label>)}
            <label className="flex items-center gap-2">
              <input type="radio" name={`catalog-${fieldID}`} checked={catalogID === null}
                disabled={isPending} onChange={() => { setCatalogID(null); setRememberAlias(false); }} />
              Keep my own name
            </label>
            <p className="text-xs text-muted-foreground">Selecting a match fills its name. You can edit the name afterward.</p>
            {catalogID && <Button type="button" variant="tertiary" size="sm" disabled={isPending}
              onClick={() => changeName(exerciseName, true)}>Search for another match</Button>}
          </fieldset>}
          {selected && <>
            <details className="rounded-lg bg-muted p-3">
              <summary className="cursor-pointer font-medium">How to do {selected.name}</summary>
              <ol className="mt-3 list-decimal space-y-2 pl-5">
                {selected.data.instructions.map((instruction, index) => <li key={index}>{instruction}</li>)}
              </ol>
            </details>
            <label className="flex items-center gap-2">
              <input type="checkbox" checked={rememberAlias} disabled={isPending}
                onChange={(event) => setRememberAlias(event.target.checked)} />
              Remember this name for my future workouts
            </label>
          </>}
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
                  disabled={isPending}
                  type="number"
                  value={set.reps}
                />
              </label>
              <label className="text-xs font-medium text-muted-foreground">
                Weight (kg)
                <input
                  className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
                  min="0"
                  disabled={isPending}
                  onChange={(event) =>
                    updateSet(index, "weight", event.target.value)
                  }
                  placeholder="Optional"
                  step="0.5"
                  type="number"
                  value={set.weight}
                />
              </label>
              {exercise && sets.length > 1 && <Button type="button" variant="tertiary" size="sm"
                disabled={isPending} className="col-span-3 justify-self-end"
                onClick={() => setSets((current) => current.filter((_, i) => i !== index))}>
                Remove set {index + 1}
              </Button>}
            </div>
          ))}
          <Button variant="tertiary" size="md"
            icon={<Plus className="h-4 w-4" />}
            onClick={() => setSets((currentSets) => [...currentSets, newSet()])}
            type="button"
            disabled={isPending || sets.length >= 20}
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
          disabled={isPending || searching}
          type="submit"
        >
          {isPending ? "Saving exercise…" : exercise ? "Save changes" : "Save exercise"}
        </Button>
        {onCancel && <Button type="button" variant="secondary" size="lg" className="w-full"
          disabled={isPending} onClick={onCancel}>Cancel</Button>}
      </form>
    </aside>
  );
}
