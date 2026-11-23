export const fitnessStyles = {
  hero: "relative isolate flex items-center overflow-hidden min-h-[clamp(12rem,23vw,16rem)] p-[clamp(1.25rem,3.5vw,3rem)] rounded-[clamp(1.75rem,4.2vw,3.25rem)] bg-(image:--fitness-hero) shadow-(--fitness-hero-shadow) text-white",
  title: "text-[clamp(3rem,7vw,6rem)] font-[850] tracking-[-.065em] leading-[.95]",
  ecg:
    "absolute -z-1 inset-bs-[50%] end-[clamp(-2rem,3vw,3rem)] w-[clamp(9rem,31vw,21rem)] aspect-square " +
    "-translate-y-1/2 opacity-48 pointer-events-none " +
    "[&_svg]:absolute [&_svg]:inset-bs-[50%] [&_svg]:w-full [&_svg]:-translate-y-1/2 " +
    "sm:opacity-90 " +
    "motion-safe:[&_path]:[stroke-dasharray:1] motion-safe:[&_path]:animate-fitness-trace",
  ripple:
    "hidden absolute inset-0 border-2 border-white/60 rounded-full " +
    "motion-safe:block motion-safe:animate-fitness-ripple",
  surface: "rounded-[clamp(1.5rem,3vw,2.25rem)] shadow-(--fitness-shadow)",
  muted: "text-(--fitness-muted)",
  selectedDate: "bg-(--fitness-chip)",
  timeChip:
    "bg-(--fitness-chip) " +
    "inline-flex items-center gap-1.5 py-1 px-3 rounded-full text-sm font-semibold",
  streakDay:
    "bg-(--fitness-chip) " +
    "data-[workout=true]:bg-(image:--fitness-gradient) data-[workout=true]:text-white " +
    "text-(--fitness-muted)",
  selectedDateDetails: "text-(--fitness-muted) text-control",
  day:
    "relative grid place-items-center min-w-0 min-h-[clamp(2.75rem,5vw,3.75rem)] rounded-[clamp(.75rem,1.5vw,1.125rem)] font-bold " +
    "[&[data-outside=true]:not([aria-pressed=true])]:text-(--fitness-muted) data-[outside=true]:font-medium " +
    "[&:not([data-workout=true],[aria-pressed=true])]:hover:bg-(--fitness-chip) " +
    "[&[data-workout=true]:not([aria-pressed=true])]:bg-(--fitness-done) " +
    "data-[workout=true]:after:absolute data-[workout=true]:after:inset-be-1 data-[workout=true]:after:w-1.25 " +
    "data-[workout=true]:after:h-1.25 data-[workout=true]:after:rounded-full " +
    "data-[workout=true]:after:bg-(--fitness-dot) " +
    "aria-[current=date]:shadow-[inset_0_0_0_2px_var(--fitness-today)] " +
    "aria-pressed:bg-(image:--fitness-gradient) aria-pressed:text-white " +
    "aria-pressed:data-[workout=true]:after:[background:white]",
  legendToday:
    "shadow-[inset_0_0_0_2px_var(--fitness-today)] " +
    "inline-block w-3.5 h-3.5 flex-none rounded-sm",
  legendSelected:
    "bg-(image:--fitness-gradient) text-white " +
    "inline-block w-3.5 h-3.5 flex-none rounded-sm",
  last:
    "bg-(image:--fitness-gradient) text-white " +
    "shadow-(--fitness-spotlight-shadow)",
  dateBadge:
    "bg-(image:--fitness-gradient) text-white " +
    "flex flex-none flex-col items-center justify-center gap-1 w-17 h-19 rounded-[1.375rem] leading-none",
  legendWorkout:
    "grid size-3.5 flex-none rounded-sm place-items-center bg-(--fitness-done) " +
    "after:w-1.25 after:h-1.25 after:rounded-full after:bg-(--fitness-dot)",
  summaryValue: "text-[clamp(1.9rem,4.4vw,3.65rem)] font-[850] tracking-[-.055em] leading-none",
  workoutDate:
    "text-(--fitness-muted) " +
    "block mt-1 mb-2 text-control",
  empty:
    "text-(--fitness-muted) " +
    "p-4.5 border-2 border-dashed border-[color-mix(in_srgb,var(--foreground)_20%,transparent)] rounded-hero text-center",
  sectionTitle: "mb-3 text-[clamp(1.25rem,2.3vw,1.75rem)] font-extrabold tracking-[-.035em]",
  tileGrid:
    "grid gap-4 " +
    "sm:grid-cols-2 " +
    "lg:grid-cols-3 lg:gap-6",
  recentGrid:
    "grid grid-cols-1 gap-[clamp(.75rem,1.8vw,1.5rem)] min-[641px]:grid-cols-2 min-[981px]:grid-cols-3",
  workoutCard: "shadow-(--fitness-shadow) flex items-center gap-4 h-full p-[clamp(.875rem,1.8vw,1.375rem)] rounded-hero",
  dateBadgeDay: "text-[1.8rem] font-extrabold tracking-[-.03em]",
  dateBadgeMonth: "text-caption font-bold uppercase",
  workoutTitle: "text-[1.25rem] font-extrabold tracking-[-.02em]",
  attendance:
    "grid place-items-center w-12 aspect-square flex-none rounded-full " +
    "[background:conic-gradient(var(--brand-blue)_var(--attendance),var(--chip)_0)] " +
    "after:w-[72%] after:aspect-square after:rounded-full after:bg-card " +
    "sm:w-14",
};
