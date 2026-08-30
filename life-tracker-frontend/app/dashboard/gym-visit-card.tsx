"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Dumbbell, Check, ArrowRight } from "lucide-react";
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
  const router = useRouter();
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
          setGymError(res.error ?? "We couldn't save your gym visit.");
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
      <article
        className="cursor-pointer rounded-xl border border-border bg-card px-4 py-3 shadow-sm"
        onClick={() => router.push("/gym-visits")}
      >
        <div className="flex items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <div
              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-secondary text-secondary-foreground"
              aria-hidden="true"
            >
              {gymVisited ? (
                <Check className="h-4 w-4 text-emerald-primary" />
              ) : (
                <Dumbbell className="h-4 w-4" />
              )}
            </div>
            <div className="min-w-0">
              <h3 className="font-semibold text-foreground">Gym visit</h3>
              <p className="truncate text-xs text-muted-foreground">
                {gymVisited ? "Completed today" : "Not logged today"}
              </p>
            </div>
          </div>
          {gymVisited && (
            <span className="shrink-0 rounded-full bg-emerald-primary/10 px-2 py-1 text-xs font-semibold text-emerald-primary">
              Done
            </span>
          )}
        </div>
        {gymError && (
          <p className="mt-3 text-sm text-destructive" role="alert">
            {gymError}
          </p>
        )}
        <div className="mt-3 flex items-center gap-3">
          <button
            className="rounded-md bg-primary px-2.5 py-1.5 text-xs font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:bg-muted disabled:text-muted-foreground"
            disabled={isMarkingGym}
            onClick={(event) => {
              event.stopPropagation();
              if (gymVisited) {
                setIsConfirmingAnotherVisit(true);
                return;
              }
              void handleMarkGymVisit();
            }}
            type="button"
          >
            {isMarkingGym ? "Marking…" : gymVisited ? "Add visit" : "Mark visited"}
          </button>
          {gymVisited && gymVisitID && (
            <Link
              className="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:opacity-80"
              href={`/gym-visits/${gymVisitID}`}
              onClick={(event) => event.stopPropagation()}
            >
              Log exercises <ArrowRight className="h-4 w-4" />
            </Link>
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
        title="Mark another gym visit?"
        description="This will create a separate visit for today, with its own exercise log."
        confirmText="Yes, mark visit"
        confirmLoadingText="Marking…"
        isLoading={isMarkingGym}
        variant="positive"
      />
    </>
  );
}
