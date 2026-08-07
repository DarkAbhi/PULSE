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
        className="cursor-pointer rounded-2xl border border-border bg-card p-6 shadow-sm"
        onClick={() => router.push("/gym-visits")}
      >
        <div
          className="flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-secondary-foreground"
          aria-hidden="true"
        >
          {gymVisited ? (
            <Check className="h-6 w-6 text-emerald-primary" />
          ) : (
            <Dumbbell className="h-6 w-6" />
          )}
        </div>
        <h3 className="mt-5 text-xl font-semibold text-foreground">
          Gym visit
        </h3>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          {gymVisited
            ? "You showed up for yourself today. Wonderful work."
            : "A little movement can make a big difference."}
        </p>
        {gymError && (
          <p className="mt-3 text-sm text-destructive" role="alert">
            {gymError}
          </p>
        )}
        <button
          className="mt-5 w-full rounded-lg bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:bg-muted disabled:text-muted-foreground"
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
          {isMarkingGym
            ? "Marking your visit…"
            : gymVisited
              ? "Mark another gym visit"
              : "Mark that I visited the gym today"}
        </button>
        {gymVisited && gymVisitID && (
          <Link
            className="mt-3 flex items-center justify-center gap-1.5 w-full rounded-lg border border-border px-4 py-3 text-center text-sm font-semibold text-primary transition hover:bg-accent"
            href={`/gym-visits/${gymVisitID}`}
            onClick={(event) => event.stopPropagation()}
          >
            Log exercises from this visit <ArrowRight className="h-4 w-4" />
          </Link>
        )}
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
