"use client";

import PulseButton from "@/app/components/design-system/pulse-button";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";

import { createContext, useContext, useEffect, useId, useRef, useState, useTransition, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { Plus, Trash2 } from "lucide-react";
import { addExercise, updateExercise, searchExercises, type CatalogSearch, type SavedExercise } from "@/app/fitness/[id]/actions";
import {
  setNumberClass, workoutLabelClass,
} from "@/app/fitness/[id]/workout-detail-classes";

const formFieldClass = "w-full min-w-0 px-3.5 max-[701px]:px-2.5";
const setEditorRowClass = "grid grid-cols-[44px_minmax(0,1fr)_minmax(0,1.2fr)_44px] items-center gap-2 max-[701px]:grid-cols-[32px_minmax(0,1fr)_minmax(0,1fr)_40px]";

type ExerciseSet = {
  reps: string;
  weight: string;
};

function newSet(): ExerciseSet {
  return { reps: "", weight: "" };
}

const OpenExerciseForm = createContext<(trigger: HTMLButtonElement) => void>(() => {});

export function AddExerciseProvider({ visitID, children }: { visitID: string; children: ReactNode }) {
  const [trigger, setTrigger] = useState<HTMLButtonElement | null>(null);

  return (
    <OpenExerciseForm.Provider value={setTrigger}>
      {children}
      {trigger && (
        <AddExerciseForm visitID={visitID} onClose={() => {
          setTrigger(null);
          requestAnimationFrame(() => {
            (trigger.isConnected ? trigger : document.getElementById("exercises-heading"))?.focus();
          });
        }} />
      )}
    </OpenExerciseForm.Provider>
  );
}

export function AddExerciseButton() {
  const openForm = useContext(OpenExerciseForm);

  return (
    <PulseButton type="button" variant="gradient" size="lg" shape="pill" labelStyle="strong"
      aria-haspopup="dialog" icon={<Plus size={20} aria-hidden="true" />}
      onClick={(event) => openForm(event.currentTarget)}>
      Add exercise
    </PulseButton>
  );
}

interface AddExerciseFormProps {
  visitID: string;
  exercise?: SavedExercise;
  onClose: () => void;
}

export default function AddExerciseForm({ visitID, exercise, onClose }: AddExerciseFormProps) {
  const fieldID = useId();
  const titleID = useId();
  const dialogRef = useRef<HTMLDivElement>(null);
  const nameRef = useRef<HTMLInputElement>(null);
  const refocusName = useRef(false);
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

  useEffect(() => {
    if (!isPending && refocusName.current) {
      refocusName.current = false;
      nameRef.current?.focus();
    }
  }, [isPending]);

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
    if (isPending || searching) return;
    setError("");
    const submitter = (event.nativeEvent as SubmitEvent).submitter;
    const addAnother = !exercise && submitter?.getAttribute("data-save-another") === "true";

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
      } else if (addAnother) {
        refocusName.current = true;
        changeName("", true);
        setSets([newSet()]);
      } else {
        onClose();
      }
    });
  }

  const form = (
    <form className="mt-6 space-y-5" onSubmit={handleSave}>
      {/* Implicit submission saves and closes, even when the footer offers both actions. */}
      {!exercise && <button type="submit" hidden disabled={isPending || searching} />}
      <div>
        <label
          className="mb-2 block text-sm font-medium text-muted-foreground"
          htmlFor={fieldID}
        >
          Exercise name
        </label>
        <input
          className={formFieldClass}
          id={fieldID}
          ref={nameRef}
          autoFocus={!exercise}
          maxLength={100}
          onChange={(event) => changeName(event.target.value)}
          placeholder="Search the library or type your own"
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
            {catalogID && <PulseButton type="button" variant="tertiary" size="sm" disabled={isPending}
              onClick={() => changeName(exerciseName, true)}>Search for a different exercise</PulseButton>}
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

      <fieldset className="min-w-0 space-y-3">
        <legend className="mb-3 font-extrabold text-foreground">Sets</legend>
        <div className={`${setEditorRowClass} ${workoutLabelClass}`} aria-hidden="true">
          <span>Set</span><span>Reps</span><span>Weight (kg), optional</span>
        </div>
        {sets.map((set, index) => (
          <div className={setEditorRowClass} key={index}>
            <span className={setNumberClass}>{index + 1}</span>
            <input
              aria-label={`Set ${index + 1} reps`}
              className={formFieldClass}
              min="1"
              onChange={(event) => updateSet(index, "reps", event.target.value)}
              required
              disabled={isPending}
              type="number"
              value={set.reps}
            />
            <input
              aria-label={`Set ${index + 1} weight (kg)`}
              className={formFieldClass}
              min="0"
              disabled={isPending}
              onChange={(event) => updateSet(index, "weight", event.target.value)}
              step="any"
              type="number"
              value={set.weight}
            />
            {sets.length > 1 && (
              <PulseButton type="button" variant="iconChipDanger" shape="pill"
                aria-label={`Remove set ${index + 1}`} disabled={isPending}
                onClick={() => setSets((current) => current.filter((_, i) => i !== index))}>
                <Trash2 size={18} aria-hidden="true" />
              </PulseButton>
            )}
          </div>
        ))}
        <PulseButton variant="chip" size="lg" shape="pill" labelStyle="strong"
          icon={<Plus size={18} aria-hidden="true" />}
          onClick={() => setSets((currentSets) => [...currentSets, newSet()])}
          type="button"
          disabled={isPending || sets.length >= 20}
        >
          Add set
        </PulseButton>
        <p className="text-muted-foreground">Leave the weight empty for a body weight exercise.</p>
      </fieldset>

      {error && (
        <p className="text-sm text-destructive" role="alert">
          {error}
        </p>
      )}
      <PulseDialogActions className="flex! flex-wrap [&>button]:w-auto [&>button]:flex-auto">
        <PulseDialogAction type="button" variant="secondary" disabled={isPending} onClick={onClose}>
          Cancel
        </PulseDialogAction>
        {!exercise && (
          <PulseDialogAction variant="secondary" data-save-another="true" disabled={isPending || searching}>
            Save and add another
          </PulseDialogAction>
        )}
        <PulseDialogAction disabled={isPending || searching}>
          {isPending ? "Saving exercise…" : exercise ? "Save changes" : "Save exercise"}
        </PulseDialogAction>
      </PulseDialogActions>
    </form>
  );

  return createPortal(
    <div ref={dialogRef} tabIndex={-1}>
      <PulseDialog labelledBy={titleID} size="lg"
        onDismiss={isPending ? undefined : onClose}>
        <PulseDialogHeader
          id={titleID}
          onClose={onClose}
          closeDisabled={isPending}
          closeAutoFocus={Boolean(exercise)}
        >
          {exercise ? "Edit exercise" : "Add exercise"}
        </PulseDialogHeader>
        {form}
      </PulseDialog>
    </div>,
    document.body,
  );
}
