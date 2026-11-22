import type { CSSProperties } from "react";
import { Dumbbell } from "lucide-react";
import type { SavedExercise } from "./actions";
import DeleteExerciseButton from "./delete-exercise-button";
import EditExerciseButton from "./edit-exercise-button";
import ExerciseInstructions from "./exercise-instructions";
import styles from "./workout-detail.module.css";

const muscleColors: Record<string, string> = {
  shoulders: "#f0603a",
  chest: "#d91cab",
  back: "#0f86c9",
  lats: "#0f86c9",
  "middle back": "#0f86c9",
  "lower back": "#0f86c9",
  biceps: "#7b2ce0",
  triceps: "#5b2ce0",
  legs: "#12a89a",
  quadriceps: "#12a89a",
  hamstrings: "#12a89a",
  calves: "#12a89a",
  glutes: "#12a89a",
  abdominals: "#d9930a",
  core: "#d9930a",
};

const weightFormat = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 3 });

export default function ExerciseCard({ visitID, exercise }: { visitID: string; exercise: SavedExercise }) {
  const catalog = exercise.catalog_exercise;
  const muscles = catalog?.data.primaryMuscles ?? [];
  const color = muscleColors[muscles[0]?.toLowerCase()] ?? "#4c6bff";
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
    <article className={styles.exercise} style={{ "--muscle-color": color } as CSSProperties}>
      <div className={styles.exerciseHeading}>
        <span className={styles.tile}><Dumbbell size={24} aria-hidden="true" /></span>
        <div>
          <h3>{exercise.name}</h3>
          {catalog && catalog.name !== exercise.name && <p>{catalog.name}</p>}
        </div>
      </div>

      <dl className={styles.exerciseMeta}>
        {metadata.map(({ label, value, dot }) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd>{dot && value && <span className={styles.dot} />}{value || "—"}</dd>
          </div>
        ))}
      </dl>

      <div>
        <div className={styles.setHeading}><span>Sets</span><span className={styles.chip}>{setCount}</span></div>
        <table className={styles.setTable} aria-label={`${exercise.name} sets`}>
          <thead>
            <tr><th scope="col">Set</th><th scope="col">Reps</th><th scope="col">Weight</th></tr>
          </thead>
          <tbody>
            {exercise.sets.map((set) => (
              <tr key={set.id}>
                <td><span className={styles.setNumber}>{set.set_number}</span></td>
                <td>{set.reps} reps</td>
                <td>{set.weight === null
                  ? <span className={styles.chip}>Body weight</span>
                  : `${weightFormat.format(set.weight)} kg`}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <footer className={styles.exerciseFooter}>
        <ExerciseInstructions
          name={catalog?.name ?? exercise.name}
          metadata={metadata.flatMap(({ value }) => value ? [value] : [])}
          instructions={catalog?.data.instructions ?? []}
        />
        <div className={styles.exerciseActions}>
          <EditExerciseButton visitID={visitID} exercise={exercise} />
          <DeleteExerciseButton visitID={visitID} exerciseID={exercise.id} exerciseName={exercise.name} />
        </div>
      </footer>
    </article>
  );
}
