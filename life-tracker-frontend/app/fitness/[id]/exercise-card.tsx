import { Dumbbell } from "lucide-react";
import type { SavedExercise } from "./actions";
import DeleteExerciseButton from "./delete-exercise-button";
import EditExerciseButton from "./edit-exercise-button";
import ExerciseInstructions from "./exercise-instructions";
import { setNumberClass, workoutChipClass, workoutLabelClass } from "./workout-detail-classes";

const muscleColorClasses: Record<string, string> = {
  shoulders: "[--muscle-color:#f0603a]",
  chest: "[--muscle-color:#d91cab]",
  back: "[--muscle-color:#0f86c9]",
  lats: "[--muscle-color:#0f86c9]",
  "middle back": "[--muscle-color:#0f86c9]",
  "lower back": "[--muscle-color:#0f86c9]",
  biceps: "[--muscle-color:#7b2ce0]",
  triceps: "[--muscle-color:#5b2ce0]",
  legs: "[--muscle-color:#12a89a]",
  quadriceps: "[--muscle-color:#12a89a]",
  hamstrings: "[--muscle-color:#12a89a]",
  calves: "[--muscle-color:#12a89a]",
  glutes: "[--muscle-color:#12a89a]",
  abdominals: "[--muscle-color:#d9930a]",
  core: "[--muscle-color:#d9930a]",
};

const weightFormat = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 3 });

export default function ExerciseCard({ visitID, exercise }: { visitID: string; exercise: SavedExercise }) {
  const catalog = exercise.catalog_exercise;
  const muscles = catalog?.data.primaryMuscles ?? [];
  const muscleColorClass = muscleColorClasses[muscles[0]?.toLowerCase()] ?? "[--muscle-color:#4c6bff]";
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
    <article className={`${muscleColorClass} grid min-w-0 grid-rows-[auto_auto_1fr_auto] gap-[18px] rounded-[28px] bg-(--workout-card) p-6 shadow-(--workout-shadow)`}>
      <div className="flex items-center gap-3.5">
        <span className="grid size-12 flex-none place-items-center rounded-2xl bg-(--muscle-color) text-white"><Dumbbell size={24} aria-hidden="true" /></span>
        <div>
          <h3 className="text-xl leading-[1.2] font-extrabold tracking-[-0.02em] wrap-anywhere">{exercise.name}</h3>
          {catalog && catalog.name !== exercise.name && <p className="mt-[3px] text-[0.85rem] wrap-anywhere text-(--workout-muted)">{catalog.name}</p>}
        </div>
      </div>

      <dl className="grid grid-cols-3 rounded-[18px] bg-(--workout-inset) max-[701px]:grid-cols-1">
        {metadata.map(({ label, value, dot }) => (
          <div key={label} className="border-(--workout-line) px-3.5 py-3 not-first:border-s max-[701px]:not-first:border-s-0 max-[701px]:not-first:border-t">
            <dt className={workoutLabelClass}>{label}</dt>
            <dd className="mt-[3px] flex items-center gap-[7px] text-[0.95rem] font-bold wrap-anywhere capitalize">{dot && value && <span className="size-[9px] flex-none rounded-full bg-(--muscle-color)" />}{value || "—"}</dd>
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
              <tr key={set.id} className="[&>td]:h-11 [&>td]:border-t [&>td]:border-(--workout-line) [&>td]:font-bold">
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

      <footer className="flex min-h-[62px] flex-wrap items-center justify-between gap-2 border-t border-(--workout-line) pt-3">
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
    </article>
  );
}
