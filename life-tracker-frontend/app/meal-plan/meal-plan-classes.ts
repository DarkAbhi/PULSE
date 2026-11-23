export const mealPlanStyles = {
  layout: "mt-3 grid gap-4",
  muted: "text-muted-foreground",
  agendaTitle:
    "font-extrabold leading-none tracking-[-.04em] " +
    "text-[clamp(1.6rem,3vw,2.4rem)]",
  weekRange:
    "font-extrabold leading-none tracking-[-.04em] " +
    "text-[clamp(1.4rem,2.4vw,2rem)]",
  primaryAction: "[--button-fill:var(--meal-action-gradient)] [--button-foreground:white]",
  weekArrow: "[--secondary:var(--meal-calendar-inset)]",
  slotAdd: "mt-auto w-full border-2 border-dashed border-(--meal-calendar-outline) [--action:var(--foreground)]",
  todayBadge:
    "bg-(image:--meal-action-gradient) text-white " +
    "flex-none py-1 px-2.5 rounded-full text-xs font-bold",
  addMeal: "shadow-(--meal-action-shadow)",
  calendar:
    "shadow-(--meal-calendar-shadow) " +
    "p-[clamp(12px,2.2vw,28px)] rounded-[clamp(26px,4vw,40px)]",
  slotCard:
    "shadow-(--meal-calendar-shadow) " +
    "flex min-w-0 flex-col gap-4 rounded-4xl p-4 sm:p-5",
  weekDays: "grid grid-cols-7 gap-[clamp(4px,1vw,14px)]",
  day:
    "flex flex-col items-center min-w-0 min-h-20 gap-1 py-2 rounded-[clamp(11px,2.4vw,28px)] bg-(--meal-calendar-inset) " +
    "[&[aria-current=date]:not([aria-pressed=true])]:shadow-[inset_0_0_0_2px_var(--meal-snack)] " +
    "aria-pressed:bg-(image:--meal-action-gradient) aria-pressed:text-white aria-pressed:shadow-(--meal-selected-shadow) " +
    "sm:min-h-23 sm:py-2.5 " +
    "group/meal-day",
  dayLabel:
    "text-muted-foreground text-[clamp(.68rem,1.2vw,.85rem)] font-semibold " +
    "group-aria-pressed/meal-day:text-white",
  dayNumber: "text-[clamp(1.25rem,2.3vw,1.8rem)] font-extrabold tabular-nums leading-none",
  dots: "flex gap-[clamp(1px,.35vw,4px)] mt-auto",
  dot:
    "w-[clamp(4px,.65vw,7px)] aspect-square rounded-full bg-(--meal-calendar-outline) " +
    "data-[planned=true]:bg-(--slot-color) " +
    "group-aria-pressed/meal-day:data-[planned=false]:bg-white/40 " +
    "group-aria-pressed/meal-day:data-[planned=true]:bg-white",
  slotIcon:
    "grid size-13 flex-none place-items-center rounded-2xl bg-(--slot-color)",
  slotGrid:
    "grid gap-[clamp(12px,1.4vw,18px)] mt-4 " +
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
