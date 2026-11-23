export const fitnessStyles = {
  fitness:
    "[--fitness-surface:#fff] [--fitness-ink:#1a1740] [--fitness-muted:#5e5a7d] [--fitness-chip:#eeedf6] " +
    "[--fitness-attendance-track:rgb(26_23_64_/_7%)] [--fitness-done:#e6ebff] [--fitness-dot:#3e58dc] " +
    "[--fitness-focus:#3e58dc] [--fitness-today:#dc229f] [--fitness-shadow:0_18px_40px_-18px_#4c6bff4d] " +
    "[--fitness-gradient:linear-gradient(135deg,#3a2cd9,#a420bd)] text-(--fitness-ink) " +
    "dark:[--fitness-surface:#1a1636] dark:[--fitness-ink:#f4f1ff] dark:[--fitness-muted:#b7b0d1] " +
    "dark:[--fitness-chip:#2b2749] dark:[--fitness-attendance-track:rgb(244_241_255_/_10%)] " +
    "dark:[--fitness-done:#193953] dark:[--fitness-dot:#6adaf7] dark:[--fitness-focus:#a5b5ff] " +
    "dark:[--fitness-today:#ff55d1] dark:[--fitness-shadow:0_18px_40px_-18px_#0008] " +
    "[&_:is(button,a):focus-visible]:outline-2 [&_:is(button,a):focus-visible]:outline-(--fitness-focus) " +
    "[&_:is(button,a):focus-visible]:outline-offset-[2px]",
  hero: "relative isolate flex items-center overflow-hidden min-h-[clamp(12rem,23vw,16rem)] p-[clamp(1.25rem,3.5vw,3rem)] rounded-[clamp(1.75rem,4.2vw,3.25rem)] [background:linear-gradient(135deg,#3a2cd9,#7b2ce0_55%,#bd199d)] shadow-[0_22px_44px_-24px_#5b2ce0d9] text-white",
  title: "text-[clamp(3rem,7vw,6rem)] font-[850] tracking-[-.065em] leading-[.95]",
  heroAction:
    "rounded-full! " +
    "min-h-[clamp(2.75rem,4vw,3.5rem)]! [background:white]! text-[#5324c4]! font-[750]! px-[clamp(1rem,2vw,1.5rem)]! " +
    "hover:[background:#f4eeff]! " +
    "focus-visible:outline-white!",
  logAction:
    "rounded-full! " +
    "[background:var(--fitness-gradient)]! text-white! " +
    "hover:[background:linear-gradient(135deg,#3020c5,#8b169f)]!",
  monthButton:
    "rounded-full! " +
    "[background:var(--fitness-chip)]! " +
    "text-(--fitness-ink)! " +
    "hover:[background:color-mix(in_srgb,var(--fitness-chip),var(--fitness-done)_35%)]!",
  ecg:
    "absolute -z-1 inset-bs-[50%] end-[clamp(-2rem,3vw,3rem)] w-[clamp(9rem,31vw,21rem)] aspect-square " +
    "-translate-y-1/2 opacity-48 pointer-events-none " +
    "[&_svg]:absolute [&_svg]:inset-bs-[50%] [&_svg]:w-full [&_svg]:-translate-y-1/2 " +
    "sm:opacity-90 " +
    "motion-safe:[&_path]:[stroke-dasharray:1] motion-safe:[&_path]:animate-fitness-trace",
  ripple:
    "hidden absolute inset-0 border-2 border-[#fff9] rounded-full " +
    "motion-safe:block motion-safe:animate-fitness-ripple",
  surface: "rounded-[clamp(1.5rem,3vw,2.25rem)] [background:var(--fitness-surface)] shadow-[var(--fitness-shadow)]",
  muted: "text-(--fitness-muted)",
  selectedDate: "[background:var(--fitness-chip)]",
  timeChip:
    "[background:var(--fitness-chip)] " +
    "inline-flex items-center gap-1.5 py-1.25 px-3 rounded-full text-[.85rem] font-semibold",
  streakDay:
    "[background:var(--fitness-chip)] " +
    "data-[workout=true]:[background:var(--fitness-gradient)] data-[workout=true]:text-white " +
    "text-(--fitness-muted)",
  selectedDateDetails: "text-(--fitness-muted) text-[.95rem]",
  day:
    "relative grid place-items-center min-w-0 min-h-[clamp(2.75rem,5vw,3.75rem)] rounded-[clamp(.75rem,1.5vw,1.125rem)] font-bold " +
    "[&[data-outside=true]:not([aria-pressed=true])]:text-(--fitness-muted) data-[outside=true]:font-medium " +
    "[&:not([data-workout=true],[aria-pressed=true])]:hover:bg-(--fitness-chip) " +
    "[&[data-workout=true]:not([aria-pressed=true])]:bg-(--fitness-done) " +
    "data-[workout=true]:after:absolute data-[workout=true]:after:inset-be-1 data-[workout=true]:after:w-[.3rem] " +
    "data-[workout=true]:after:h-[.3rem] data-[workout=true]:after:rounded-full " +
    "data-[workout=true]:after:[background:var(--fitness-dot)] " +
    "aria-[current=date]:shadow-[inset_0_0_0_2px_var(--fitness-today)] " +
    "aria-pressed:[background:var(--fitness-gradient)] aria-pressed:text-white " +
    "aria-pressed:data-[workout=true]:after:[background:white]",
  legendToday:
    "shadow-[inset_0_0_0_2px_var(--fitness-today)] " +
    "inline-block w-3.5 h-3.5 flex-none rounded-[.3rem]",
  legendSelected:
    "[background:var(--fitness-gradient)] text-white " +
    "inline-block w-3.5 h-3.5 flex-none rounded-[.3rem]",
  last:
    "[background:var(--fitness-gradient)] text-white " +
    "shadow-[0_20px_40px_-22px_#5b2ce0d9]",
  dateBadge:
    "[background:var(--fitness-gradient)] text-white " +
    "flex flex-none flex-col items-center justify-center gap-1 w-17 h-19 rounded-[1.375rem] leading-none",
  legendWorkout:
    "grid size-3.5 flex-none rounded-[.3rem] place-items-center [background:var(--fitness-done)] " +
    "after:w-[.3rem] after:h-[.3rem] after:rounded-full after:[background:var(--fitness-dot)]",
  summaryValue: "text-[clamp(1.9rem,4.4vw,3.65rem)] font-[850] tracking-[-.055em] leading-none",
  workoutDate:
    "text-(--fitness-muted) " +
    "block [margin-block:.1875rem_.5rem] text-[.95rem]",
  empty:
    "text-(--fitness-muted) " +
    "p-4.5 border-2 border-dashed border-[color-mix(in_srgb,var(--fitness-ink)_20%,transparent)] rounded-[2rem] text-center",
  sectionTitle: "mb-3 text-[clamp(1.25rem,2.3vw,1.75rem)] font-extrabold tracking-[-.035em]",
  tileGrid:
    "grid gap-4 " +
    "sm:grid-cols-2 " +
    "lg:grid-cols-3 lg:gap-6",
  recentGrid:
    "grid grid-cols-1 gap-[clamp(.75rem,1.8vw,1.5rem)] min-[641px]:grid-cols-2 min-[981px]:grid-cols-3",
  workoutCard: "flex items-center gap-4 h-full p-[clamp(.875rem,1.8vw,1.375rem)] rounded-[2rem]!",
  dateBadgeDay: "text-[1.8rem] font-extrabold tracking-[-.03em]",
  dateBadgeMonth: "text-[.8rem] font-bold uppercase",
  workoutTitle: "text-[1.25rem] font-extrabold tracking-[-.02em]",
  attendance:
    "grid place-items-center w-12 aspect-square flex-none rounded-full " +
    "[background:conic-gradient(#4c6bff_var(--attendance),var(--fitness-attendance-track)_0)] " +
    "after:w-[72%] after:aspect-square after:rounded-full after:[background:var(--fitness-surface)] " +
    "sm:w-14",
};
