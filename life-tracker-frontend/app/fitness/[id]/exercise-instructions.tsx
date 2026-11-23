"use client";

import { useId, useRef } from "react";
import { Info } from "lucide-react";
import Button from "@/app/components/design-system/button";
import { workoutChipClass, workoutPrimaryClass, workoutSoftClass } from "./workout-detail-classes";

const instructionsDialogClass = [
  "inset-0 m-auto max-h-[calc(100dvh-24px)] w-[min(560px,calc(100%-24px))] overflow-auto overscroll-contain rounded-[32px] bg-(--workout-card) text-(--workout-ink) shadow-[0_30px_80px_-20px_rgb(0_0_0/55%)]",
  "backdrop:bg-[#0d0a1f]/55 backdrop:backdrop-blur-[4px]",
  "max-[701px]:mx-0 max-[701px]:mb-0 max-[701px]:w-full max-[701px]:max-w-none max-[701px]:rounded-t-[28px] max-[701px]:rounded-b-none",
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
      <Button type="button" variant="secondary" className={workoutSoftClass}
        aria-haspopup="dialog" onClick={() => dialogRef.current?.showModal()}
        icon={<Info size={18} aria-hidden="true" />}>
        Instructions
      </Button>
      <dialog ref={dialogRef} className={instructionsDialogClass} aria-labelledby={titleID}
        onClick={(event) => {
          if (event.target === event.currentTarget) dialogRef.current?.close();
        }}>
        <div className="grid gap-[18px] p-7">
          <h2 id={titleID} className="text-[1.6rem] leading-[1.15] font-extrabold tracking-[-0.03em]">{name}</h2>
          {metadata.length > 0 && (
            <div className="flex flex-wrap gap-2 capitalize">
              {metadata.map((value, index) => <span className={workoutChipClass} key={index}>{value}</span>)}
            </div>
          )}
          {instructions.length > 0 ? (
            <ol className="grid gap-3.5 [counter-reset:step]">
              {instructions.map((step, index) => <li key={index} className="flex items-start gap-3.5 [counter-increment:step] before:grid before:size-[30px] before:flex-none before:place-items-center before:rounded-full before:bg-[linear-gradient(135deg,#3a2cd9,#b22bd8)] before:text-[0.85rem] before:font-extrabold before:text-white before:content-[counter(step)]">
                  {step}
                </li>)}
            </ol>
          ) : <p className="text-(--workout-muted)">No instructions are available for this exercise.</p>}
          <form method="dialog">
            <Button type="submit" className={`${workoutPrimaryClass} w-full`}>Got it</Button>
          </form>
        </div>
      </dialog>
    </>
  );
}
