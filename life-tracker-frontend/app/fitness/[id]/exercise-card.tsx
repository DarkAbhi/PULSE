import { Dumbbell } from "lucide-react";
import PulseCard from "@/app/components/design-system/pulse-card";
import type { SavedExercise } from "@/app/fitness/[id]/actions";
import DeleteExerciseButton from "@/app/fitness/[id]/delete-exercise-button";
import EditExerciseButton from "@/app/fitness/[id]/edit-exercise-button";
import ExerciseInstructions from "@/app/fitness/[id]/exercise-instructions";
import { setNumberClass, workoutChipClass, workoutLabelClass } from "@/app/fitness/[id]/workout-detail-classes";

const muscleColorClasses: Record<string, string> = {
  shoulders: "[--muscle-color:var(--muscle-shoulders)]",
  chest: "[--muscle-color:var(--muscle-chest)]",
  back: "[--muscle-color:var(--muscle-back)]",
  lats: "[--muscle-color:var(--muscle-back)]",
  "middle back": "[--muscle-color:var(--muscle-back)]",
  "lower back": "[--muscle-color:var(--muscle-back)]",
  biceps: "[--muscle-color:var(--muscle-biceps)]",
  triceps: "[--muscle-color:var(--muscle-triceps)]",
  legs: "[--muscle-color:var(--muscle-legs)]",
  quadriceps: "[--muscle-color:var(--muscle-legs)]",
  hamstrings: "[--muscle-color:var(--muscle-legs)]",
  calves: "[--muscle-color:var(--muscle-legs)]",
  glutes: "[--muscle-color:var(--muscle-legs)]",
  abdominals: "[--muscle-color:var(--muscle-core)]",
  core: "[--muscle-color:var(--muscle-core)]",
};

const weightFormat = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 3 });

export default function ExerciseCard({ visitID, exercise }: { visitID: string; exercise: SavedExercise }) {
  const catalog = exercise.catalog_exercise;
  const muscles = catalog?.data.primaryMuscles ?? [];
  const muscleColorClass = muscleColorClasses[muscles[0]?.toLowerCase()] ?? "[--muscle-color:var(--brand-blue)]";
  const equipment = catalog?.equipment;
  const weightType = exercise.sets.length > 0 && exercise.sets.every((set) => set.weight == null)
    ? "Body weight"
    : equipment === "body only" ? "Weighted" : equipment;
  const setCount = `${exercise.sets.length} ${exercise.sets.length === 1 ? "set" : "sets"}`;
  const metadata = [
    { label: "Category", value: catalog?.data.category },
    { label: "Weight type", value: weightType },
    { label: "Body part", value: muscles.join(", "), dot: true },
  ];

  return (
    <PulseCard as="article" className={`${muscleColorClass} grid grid-rows-[auto_auto_1fr_auto] gap-4`} padding="md">
      <div className="flex items-center gap-3.5">
        <span className="grid size-12 flex-none place-items-center rounded-2xl bg-(--muscle-color) text-white"><Dumbbell size={24} aria-hidden="true" /></span>
        <div>
          <h3 className="text-xl leading-[1.2] font-extrabold tracking-[-0.02em] wrap-anywhere">{exercise.name}</h3>
          {catalog && catalog.name !== exercise.name && <p className="mt-1 text-[0.85rem] wrap-anywhere text-muted-foreground">{catalog.name}</p>}
        </div>
      </div>

      <dl className="grid grid-cols-3 rounded-2xl bg-muted max-[701px]:grid-cols-1">
        {metadata.map(({ label, value, dot }) => (
          <div key={label} className="border-divider px-3.5 py-3 not-first:border-s max-[701px]:not-first:border-s-0 max-[701px]:not-first:border-t">
            <dt className={workoutLabelClass}>{label}</dt>
            <dd className="mt-1 flex items-center gap-2 text-control font-bold wrap-anywhere capitalize">{dot && value && <span className="size-2 flex-none rounded-full bg-(--muscle-color)" />}{value || "—"}</dd>
          </div>
        ))}
      </dl>

      <div>
        <div className="mb-2 flex items-center justify-between font-extrabold"><span>Sets</span><span className={workoutChipClass}>{setCount}</span></div>
        <table className="w-full text-start tabular-nums [&_:is(th,td)]:px-1 [&_:is(th,td)]:text-start [&_:is(th,td):first-child]:w-12 [&_:is(th,td):last-child]:w-[calc((100%-48px)/2)] [&_:is(th,td):last-child]:text-end" aria-label={`${exercise.name} sets`}>
          <thead>
            <tr className={`${workoutLabelClass} [&>th]:h-8`}>
              <th scope="col">Set</th><th scope="col">Reps</th><th scope="col">Weight</th>
            </tr>
          </thead>
          <tbody>
            {exercise.sets.map((set) => (
              <tr key={set.id} className="[&>td]:h-11 [&>td]:border-t [&>td]:border-divider [&>td]:font-bold">
                <td><span className={setNumberClass}>{set.set_number}</span></td>
                <td>{set.reps} reps</td>
                <td>{set.weight === null
                  ? <span className={workoutChipClass}>Body weight</span>
                  : `${weightFormat.format(set.weight)} kg`}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <footer className="flex min-h-16 flex-wrap items-center justify-between gap-2 border-t border-divider pt-3">
        <ExerciseInstructions
          name={catalog?.name ?? exercise.name}
          metadata={metadata.flatMap(({ value }) => value ? [value] : [])}
          instructions={catalog?.data.instructions ?? []}
        />
        <div className="ms-auto flex gap-0.5">
          <EditExerciseButton visitID={visitID} exercise={exercise} />
          <DeleteExerciseButton visitID={visitID} exerciseID={exercise.id} exerciseName={exercise.name} />
        </div>
      </footer>
    </PulseCard>
  );
}
