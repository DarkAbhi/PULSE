"use client";

import PulseCard from "@/app/components/design-system/pulse-card";

import Button from "@/app/components/design-system/button";
import Dialog, { DialogAction, DialogActions } from "@/app/components/design-system/dialog";

import { useEffect, useId, useRef, useState, useTransition } from "react";
import { createPortal } from "react-dom";
import { Plus, X } from "lucide-react";
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
  onClose?: () => void;
}

export default function AddExerciseForm({ visitID, exercise, onClose }: AddExerciseFormProps) {
  const fieldID = useId();
  const titleID = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const [exerciseName, setExerciseName] = useState(exercise?.name ?? "");
  const [searchRequest, setSearchRequest] = useState<{ name: string } | null>(() => {
    const name = exercise?.name.trim();
    return name && !exercise?.exercise_catalog_id ? { name } : null;
  });
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
  const [searching, setSearching] = useState(searchRequest !== null);
  const [searchError, setSearchError] = useState("");

  useEffect(() => {
    if (!searchRequest) return;
    let cancelled = false;
    const timer = setTimeout(async () => {
      const response = await searchExercises(searchRequest.name).catch(() => ({
        result: undefined,
        error: "We couldn't search exercises. You can still save your own name.",
      }));
      if (cancelled) return;
      setCatalog(response.result ?? null);
      setCatalogID(response.result?.exercise_catalog_id ?? null);
      setSearchError(response.error ?? "");
      setSearching(false);
    }, 300);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [searchRequest]);

  function changeName(value: string, resetMatch = false) {
    setExerciseName(value);
    setRememberAlias(false);
    if (!resetMatch && catalogID !== null && searchRequest === null) return;
    const name = value.trim();
    // A retry must start a new request even if the name hasn't changed.
    setSearchRequest(name ? { name } : null);
    setCatalog(null);
    setCatalogID(null);
    setSearchError("");
    setSearching(Boolean(name));
  }

  function selectMatch(id: string, name: string) {
    setExerciseName(name);
    setSearchRequest(null);
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

    dialogRef.current?.focus();
    startTransition(async () => {
      const res = exercise
        ? await updateExercise(visitID, exercise.id, exerciseName, payloadSets, catalogID, rememberAlias)
        : await addExercise(visitID, exerciseName, payloadSets, catalogID, rememberAlias);
      if (!res.ok) {
        setError(res.error ?? "We couldn't save that exercise.");
      } else {
        if (exercise) { onClose?.(); return; }
        changeName("", true);
        setSets([newSet()]);
      }
    });
  }

  function handleDialogKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Escape" && !isPending) {
      event.preventDefault();
      onClose?.();
    }

    if (event.key !== "Tab") return;
    const controls = Array.from(event.currentTarget.querySelectorAll<HTMLElement>(
      "button:enabled, input:enabled, summary",
    )).filter((control) => control.getClientRects().length > 0 &&
      (!(control instanceof HTMLInputElement) || control.type !== "radio" || control.checked));
    const first = controls[0];
    const last = controls.at(-1);

    if (document.activeElement === event.currentTarget) {
      event.preventDefault();
      (event.shiftKey ? last : first)?.focus();
    } else if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  }

  const form = (
    <form className="mt-6 space-y-5" onSubmit={handleSave}>
      <div>
        <label
          className="mb-2 block text-sm font-medium text-muted-foreground"
          htmlFor={fieldID}
        >
          Exercise name
        </label>
        <input
          className="w-full rounded-lg bg-secondary text-foreground px-4 py-3 transition"
          id={fieldID}
          maxLength={100}
          onChange={(event) => changeName(event.target.value)}
          placeholder="e.g. Barbell squat"
          required
          disabled={isPending}
          value={exerciseName}
        />
      </div>

      <div className="space-y-3 text-sm">
        <div role="status" className="text-muted-foreground">
          {searching && <p>Searching the exercise catalog…</p>}
          {searchError && <p>{searchError}</p>}
          {catalog?.candidates.length === 0 && (
            <p>No catalog matches. You can save this as a custom exercise.</p>
          )}
        </div>

        {catalog && catalog.candidates.length > 0 && (
          <fieldset aria-describedby={`${fieldID}-catalog-help`} className="min-w-0 space-y-2">
            <legend className="font-semibold text-foreground">Choose an exercise</legend>
            <p id={`${fieldID}-catalog-help`} className="pb-1 text-muted-foreground">
              Pick a catalog match to fill in its name and get instructions, or use your own exercise.
            </p>
            {catalog.candidates.map((candidate) => (
              <label key={candidate.id}
                className="flex cursor-pointer items-start gap-3 rounded-xl border border-divider px-3 py-3 hover:bg-secondary has-[:checked]:border-primary has-[:checked]:bg-primary/10"
              >
                <input className="mt-0.5 shrink-0" type="radio" name={`catalog-${fieldID}`}
                  checked={catalogID === candidate.id} disabled={isPending}
                  onChange={() => selectMatch(candidate.id, candidate.name)}
                  onClick={() => selectMatch(candidate.id, candidate.name)}
                />
                <span className="min-w-0 break-words">
                  <span className="block font-medium text-foreground">{candidate.name}</span>
                  <span className="block text-xs text-muted-foreground">
                    {[candidate.equipment ?? "Equipment unspecified", ...candidate.data.primaryMuscles].join(" · ")}
                  </span>
                </span>
              </label>
            ))}
            <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-divider px-3 py-3 hover:bg-secondary has-[:checked]:border-primary has-[:checked]:bg-primary/10">
              <input className="mt-0.5 shrink-0" type="radio" name={`catalog-${fieldID}`}
                checked={catalogID === null} disabled={isPending}
                onChange={() => { setCatalogID(null); setRememberAlias(false); }}
              />
              <span>
                <span className="block font-medium text-foreground">Use my own exercise</span>
                <span className="block text-xs text-muted-foreground">
                  Save the name above as a custom exercise.
                </span>
              </span>
            </label>
            {catalogID && <Button type="button" variant="tertiary" size="sm" disabled={isPending}
              onClick={() => changeName(exerciseName, true)}>Search for a different exercise</Button>}
          </fieldset>
        )}

        {selected && <>
          <details className="rounded-lg bg-muted p-3">
            <summary className="cursor-pointer font-medium">How to do {selected.name}</summary>
            <ol className="mt-3 list-decimal space-y-2 pl-5">
              {selected.data.instructions.map((instruction, index) => <li key={index}>{instruction}</li>)}
            </ol>
          </details>
          <label className="flex items-start gap-2">
            <input className="mt-0.5 shrink-0" type="checkbox" checked={rememberAlias} disabled={isPending}
              onChange={(event) => setRememberAlias(event.target.checked)} />
            Match the name above to this exercise in future workouts
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
                className="mt-1 w-full rounded-lg bg-secondary text-foreground px-3 py-2 text-sm"
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
                className="mt-1 w-full rounded-lg bg-secondary text-foreground px-3 py-2 text-sm"
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
      {exercise ? (
        <DialogActions>
          <DialogAction type="button" variant="secondary" disabled={isPending} onClick={onClose}>
            Cancel
          </DialogAction>
          <DialogAction disabled={isPending || searching}>
            {isPending ? "Saving exercise…" : "Save changes"}
          </DialogAction>
        </DialogActions>
      ) : (
        <Button variant="primary" size="lg" className="w-full"
          disabled={isPending || searching} type="submit">
          {isPending ? "Saving exercise…" : "Save exercise"}
        </Button>
      )}
    </form>
  );

  if (exercise) {
    return createPortal(
      <div ref={dialogRef} tabIndex={-1} onKeyDown={handleDialogKeyDown}>
        <Dialog labelledBy={titleID} size="md"
          onBackdropClick={(event) => {
            if (event.target === event.currentTarget && !isPending) onClose?.();
          }}>
          <div className="flex items-center justify-between gap-3 border-b border-divider pb-4">
            <h2 id={titleID} className="text-xl font-bold text-foreground">Edit exercise</h2>
            <Button autoFocus type="button" variant="icon" size="sm"
              aria-label="Close exercise editor" disabled={isPending} onClick={onClose}>
              <X className="h-5 w-5" />
            </Button>
          </div>
          {form}
        </Dialog>
      </div>,
      document.body,
    );
  }

  return (
    <PulseCard as="aside" className="h-fit p-6 sm:p-8">
      <h2 className="text-xl font-semibold text-foreground">Add an exercise</h2>
      {form}
    </PulseCard>
  );
}
