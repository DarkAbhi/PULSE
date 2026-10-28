"use client";

import Button from "../components/design-system/button";

import { useState, useTransition } from "react";
import Link from "next/link";
import { Dumbbell, ArrowRight } from "lucide-react";
import ConfirmationDialog from "../components/design-system/confirmation-dialog";
import { markGymVisitAction } from "./actions";

interface GymVisitCardProps {
  initialVisited: boolean;
  initialVisitID: number | null;
}

export default function GymVisitCard({
  initialVisited,
  initialVisitID,
}: GymVisitCardProps) {
  const [gymVisited, setGymVisited] = useState(initialVisited);
  const [gymVisitID, setGymVisitID] = useState<number | null>(initialVisitID);
  const [isMarkingGym, startMarkingGym] = useTransition();
  const [gymError, setGymError] = useState("");
  const [isConfirmingAnotherVisit, setIsConfirmingAnotherVisit] =
    useState(false);

  async function handleMarkGymVisit(): Promise<boolean> {
    setGymError("");
    let success = false;

    // We use a promise wrapping the transition to resolve boolean success
    await new Promise<void>((resolve) => {
      startTransition(async () => {
        const res = await markGymVisitAction();
        if (!res.ok) {
          setGymError(res.error ?? "We couldn't save your workout.");
          success = false;
        } else {
          setGymVisited(true);
          setGymVisitID(res.id ?? null);
          success = true;
        }
        resolve();
      });
    });

    return success;
  }

  // Wrapper for startTransition helper since useTransition doesn't return value
  function startTransition(cb: () => Promise<void>) {
    startMarkingGym(() => cb());
  }

  return (
    <>
      <article className="pulse-summary-card pulse-fitness-card">
        <span className="pulse-card-icon" aria-hidden="true"><Dumbbell className="h-6 w-6" /></span>
        <p className="pulse-card-label">Fitness · Today</p>
        <h2>{gymVisited ? "Workout recorded" : "No workout recorded today"}</h2>
        {gymError && <p className="pulse-card-message" role="alert">{gymError}</p>}
        <div className="pulse-card-footer">
          <Link className="pulse-card-cta" href="/fitness">{gymVisited ? "View fitness" : "Get moving"}<ArrowRight className="h-4 w-4" /></Link>
          <img src="/dashboard/fitness.webp" width="240" height="160" alt="" className="pulse-card-art" />
        </div>
        <div className="pulse-workout-actions">
          <Button variant="tertiary" size="sm" className="pulse-workout-button" disabled={isMarkingGym}
            onClick={() => {
              if (gymVisited) { setIsConfirmingAnotherVisit(true); return; }
              void handleMarkGymVisit();
            }} type="button">
            {isMarkingGym ? "Marking…" : gymVisited ? "Add workout" : "Mark workout completed"}
          </Button>
          {gymVisited && gymVisitID && (
            <Link className="pulse-exercise-link" href={`/fitness/${gymVisitID}`}>Log exercises <ArrowRight className="h-4 w-4" /></Link>
          )}
        </div>
      </article>

      <ConfirmationDialog
        isOpen={isConfirmingAnotherVisit}
        onClose={() => setIsConfirmingAnotherVisit(false)}
        onConfirm={async () => {
          if (await handleMarkGymVisit()) {
            setIsConfirmingAnotherVisit(false);
          }
        }}
        title="Mark another workout completed?"
        description="This will create a separate workout for today, with its own exercise log."
        confirmText="Yes, mark workout completed"
        confirmLoadingText="Marking…"
        isLoading={isMarkingGym}
        variant="positive"
      />
    </>
  );
}
