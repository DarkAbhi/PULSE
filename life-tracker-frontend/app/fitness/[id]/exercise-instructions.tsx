"use client";

import { useId, useRef } from "react";
import { Info } from "lucide-react";
import Button from "@/app/components/design-system/button";
import styles from "./workout-detail.module.css";

export default function ExerciseInstructions({ name, metadata, instructions }: {
  name: string;
  metadata: string[];
  instructions: string[];
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleID = useId();

  return (
    <>
      <Button type="button" variant="secondary" className={`${styles.pill} ${styles.soft}`}
        aria-haspopup="dialog" onClick={() => dialogRef.current?.showModal()}
        icon={<Info size={18} aria-hidden="true" />}>
        Instructions
      </Button>
      <dialog ref={dialogRef} className={styles.instructionsDialog} aria-labelledby={titleID}
        onClick={(event) => {
          if (event.target === event.currentTarget) dialogRef.current?.close();
        }}>
        <div className={styles.instructionsBody}>
          <h2 id={titleID}>{name}</h2>
          {metadata.length > 0 && (
            <div className={styles.instructionMetadata}>
              {metadata.map((value, index) => <span className={styles.chip} key={index}>{value}</span>)}
            </div>
          )}
          {instructions.length > 0 ? (
            <ol className={styles.steps}>
              {instructions.map((step, index) => <li key={index}>{step}</li>)}
            </ol>
          ) : <p className={styles.muted}>No instructions are available for this exercise.</p>}
          <form method="dialog">
            <Button type="submit" className={`${styles.pill} ${styles.primary} w-full`}>Got it</Button>
          </form>
        </div>
      </dialog>
    </>
  );
}
