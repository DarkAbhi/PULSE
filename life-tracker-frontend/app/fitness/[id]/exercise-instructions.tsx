"use client";

import { useId, useRef } from "react";
import { Info } from "lucide-react";
import PulseButton from "@/app/components/design-system/pulse-button";
import { PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import { workoutChipClass } from "@/app/fitness/[id]/workout-detail-classes";

const instructionsDialogClass = [
  "inset-0 m-auto max-h-[calc(100dvh-1.5rem)] w-[calc(100%-1.5rem)] max-w-xl overflow-auto overscroll-contain rounded-4xl bg-card text-foreground shadow-dialog",
  "backdrop:bg-scrim/55 backdrop:backdrop-blur-xs",
  "max-[701px]:mx-0 max-[701px]:mb-0 max-[701px]:w-full max-[701px]:max-w-none max-[701px]:rounded-t-card max-[701px]:rounded-b-none",
].join(" ");

export default function ExerciseInstructions({ name, metadata, instructions }: {
  name: string;
  metadata: string[];
  instructions: string[];
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleID = useId();

  return (
    <>
      <PulseButton type="button" variant="chip" size="lg" shape="pill" labelStyle="strong"
        aria-haspopup="dialog" onClick={() => dialogRef.current?.showModal()}
        icon={<Info size={18} aria-hidden="true" />}>
        Instructions
      </PulseButton>
      <dialog ref={dialogRef} className={instructionsDialogClass} aria-labelledby={titleID}
        onClick={(event) => {
          if (event.target === event.currentTarget) dialogRef.current?.close();
        }}>
        <div className="grid gap-4 p-7">
          <PulseDialogHeader id={titleID}>{name}</PulseDialogHeader>
          {metadata.length > 0 && (
            <div className="flex flex-wrap gap-2 capitalize">
              {metadata.map((value, index) => <span className={workoutChipClass} key={index}>{value}</span>)}
            </div>
          )}
          {instructions.length > 0 ? (
            <ol className="grid gap-3.5 [counter-reset:step]">
              {instructions.map((step, index) => <li key={index} className="flex items-start gap-3.5 [counter-increment:step] before:grid before:size-8 before:flex-none before:place-items-center before:rounded-full before:bg-(image:--workout-step-gradient) before:text-sm before:font-extrabold before:text-white before:content-[counter(step)]">
                  {step}
                </li>)}
            </ol>
          ) : <p className="text-muted-foreground">No instructions are available for this exercise.</p>}
          <form method="dialog">
            <PulseButton type="submit" variant="gradient" size="lg" shape="pill" labelStyle="strong" className="w-full">Got it</PulseButton>
          </form>
        </div>
      </dialog>
    </>
  );
}
