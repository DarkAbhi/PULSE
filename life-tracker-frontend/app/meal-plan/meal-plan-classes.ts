export const mealPlanStyles = {
  layout: "mt-3 grid gap-4 sm:gap-6",
  muted: "text-muted-foreground",
  hero: "relative isolate flex items-center min-h-50 overflow-hidden p-[clamp(22px,3vw,40px)] rounded-[clamp(28px,4vw,48px)] bg-(image:--meal-hero-gradient) text-white shadow-(--meal-hero-shadow)",
  title:
    "font-extrabold leading-none tracking-[-.04em] " +
    "text-[clamp(2.75rem,6.4vw,5rem)]",
  agendaTitle:
    "font-extrabold leading-none tracking-[-.04em] " +
    "text-[clamp(1.85rem,3.4vw,2.8rem)]",
  weekRange:
    "font-extrabold leading-none tracking-[-.04em] " +
    "text-[clamp(1.4rem,2.4vw,2rem)]",
  plate:
    "absolute end-[clamp(-38px,calc(13vw_-_87px),50px)] top-1/2 -z-1 w-[clamp(170px,24vw,280px)] aspect-square " +
    "-translate-y-1/2 pointer-events-none " +
    "[&_svg]:absolute [&_svg]:inset-[14%] [&_svg]:w-[72%] motion-safe:[&_svg]:animate-pulse-breathe " +
    "[@media(max-width:639px)]:opacity-33",
  ripple:
    "absolute inset-0 border-2 border-white/55 rounded-full animate-pulse-ripple " +
    "motion-reduce:hidden",
  preferences: "mt-4 border border-white/60 [--btn-cancel-bg:var(--meal-hero-control)] [--btn-cancel-hover:var(--meal-hero-control-hover)] [--btn-cancel-text:white]",
  primaryAction: "[--button-fill:var(--meal-action-gradient)] [--button-foreground:white]",
  weekArrow: "[--secondary:var(--meal-calendar-inset)]",
  slotAdd: "mt-auto w-full border-2 border-dashed border-(--meal-calendar-outline) [--action:var(--foreground)]",
  todayBadge:
    "bg-(image:--meal-action-gradient) text-white " +
    "flex-none py-1 px-2.5 rounded-full text-xs font-bold",
  addMeal: "min-h-13 px-6 shadow-(--meal-action-shadow)",
  calendar:
    "shadow-(--meal-calendar-shadow) " +
    "p-[clamp(12px,2.2vw,28px)] rounded-[clamp(26px,4vw,40px)]",
  slotCard:
    "shadow-(--meal-calendar-shadow) " +
    "flex min-w-0 flex-col gap-4 rounded-4xl p-4 sm:p-5",
  weekDays: "grid grid-cols-7 gap-[clamp(4px,1vw,14px)]",
  day:
    "flex flex-col items-center min-w-0 min-h-21 gap-1 py-2.5 rounded-[clamp(11px,2.4vw,28px)] bg-(--meal-calendar-inset) " +
    "[&[aria-current=date]:not([aria-pressed=true])]:shadow-[inset_0_0_0_2px_var(--meal-snack)] " +
    "aria-pressed:bg-(image:--meal-action-gradient) aria-pressed:text-white aria-pressed:shadow-(--meal-selected-shadow) " +
    "sm:min-h-29 sm:py-3.5 " +
    "group/meal-day",
  dayLabel:
    "text-muted-foreground text-[clamp(.68rem,1.2vw,.85rem)] font-semibold " +
    "group-aria-pressed/meal-day:text-white",
  dayNumber: "text-[clamp(1.4rem,2.6vw,2.1rem)] font-extrabold tabular-nums leading-none",
  dots: "flex gap-[clamp(1px,.35vw,4px)] mt-auto",
  dot:
    "w-[clamp(4px,.65vw,7px)] aspect-square rounded-full bg-(--meal-calendar-outline) " +
    "data-[planned=true]:bg-(--slot-color) " +
    "group-aria-pressed/meal-day:data-[planned=false]:bg-white/40 " +
    "group-aria-pressed/meal-day:data-[planned=true]:bg-white",
  slotIcon:
    "grid size-13 flex-none place-items-center rounded-2xl bg-(--slot-color)",
  slotGrid:
    "grid gap-[clamp(12px,1.6vw,22px)] mt-[clamp(14px,2vw,22px)] " +
    "sm:grid-cols-2 " +
    "min-[1180px]:grid-cols-4",
  mealRow: "flex items-center gap-0.5 p-1 rounded-[20px] bg-(--meal-calendar-inset)",
  mealToggle:
    "flex min-w-0 min-h-11 flex-1 items-center gap-2 p-1 rounded-[16px] text-start " +
    "group/meal-toggle",
  check:
    "grid flex-none place-items-center rounded-full " +
    "size-8 border-2 border-(--meal-calendar-outline) " +
    "group-aria-checked/meal-toggle:border-transparent " +
    "group-aria-checked/meal-toggle:[background:var(--meal-action-gradient)_border-box] " +
    "group-aria-checked/meal-toggle:text-white",
  emptyState: "col-span-full py-4 px-0 text-muted-foreground",
  recipeResults:
    "max-h-60 overflow-y-auto overscroll-contain border border-divider rounded-xl " +
    "[&_li_+_li]:border-t [&_li_+_li]:border-divider",
  recipeResult:
    "flex items-center justify-between gap-3 w-full min-h-12 py-2.5 px-3 cursor-pointer text-start " +
    "hover:bg-secondary " +
    "focus-visible:bg-secondary",
};
