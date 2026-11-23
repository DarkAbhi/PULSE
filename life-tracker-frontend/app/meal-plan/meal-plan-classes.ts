export const mealPlanStyles = {
  page:
    "[--meal-surface:#fff] [--meal-text:#1a1740] [--meal-muted:#5e5a7d] [--meal-soft:#f0eff4] " +
    "[--meal-outline:#b6b1c6] [--meal-shadow:0_18px_40px_-18px_rgb(76_107_255_/_35%)] " +
    "[--meal-gradient:linear-gradient(135deg,#b81780,#c93532)] text-(--meal-text) " +
    "dark:[--meal-surface:#1a1636] dark:[--meal-text:#f4f1ff] dark:[--meal-muted:#b5afd0] " +
    "dark:[--meal-soft:#2e2949] dark:[--meal-outline:#746d90] " +
    "dark:[--meal-shadow:0_18px_40px_-24px_rgb(0_0_0_/_70%)] " +
    "[&_:is(button,a):focus-visible]:outline-3 [&_:is(button,a):focus-visible]:outline-[#4c6bff] [&_:is(button,a):focus-visible]:outline-offset-[3px]",
  layout:
    "grid gap-[clamp(16px,2vw,26px)] mt-3 " +
    "[&_button:disabled]:opacity-55 " +
    "[&_button:enabled]:cursor-pointer",
  muted: "text-(--meal-muted)",
  hero: "relative isolate flex items-center min-h-50 overflow-hidden p-[clamp(22px,3vw,40px)] rounded-[clamp(28px,4vw,48px)] [background:linear-gradient(135deg,#b81780,#e5483f)] text-white shadow-[0_22px_44px_-24px_rgb(201_26_142_/_85%)]",
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
    "absolute end-[clamp(-38px,calc(13vw_-_87px),50px)] top-[50%] -z-1 w-[clamp(170px,24vw,280px)] aspect-square " +
    "-translate-y-1/2 pointer-events-none " +
    "[&_svg]:absolute [&_svg]:inset-[14%] [&_svg]:w-[72%] motion-safe:[&_svg]:animate-pulse-breathe " +
    "[@media(max-width:639px)]:opacity-33",
  ripple:
    "absolute inset-0 border-2 border-[rgb(255_255_255_/_55%)] rounded-full animate-pulse-ripple " +
    "motion-reduce:hidden",
  preferences:
    "inline-flex items-center justify-center min-h-12 gap-2 rounded-full font-[750] " +
    "mt-4.5 px-4 border-[1.5px] border-[rgb(255_255_255_/_60%)] [background:rgb(64_0_45_/_25%)] " +
    "hover:[background:rgb(64_0_45_/_40%)]",
  primaryAction:
    "inline-flex items-center justify-center gap-2 rounded-full bg-(image:--meal-gradient) font-[750] text-white",
  todayAction: "min-h-12 px-[clamp(12px,2vw,20px)]",
  weekArrow:
    "inline-flex items-center justify-center min-h-12 gap-2 rounded-full font-[750] " +
    "w-12 bg-(--meal-soft)",
  slotAdd:
    "inline-flex items-center justify-center min-h-12 gap-2 rounded-full font-[750] " +
    "w-full mt-auto border-2 border-dashed border-(--meal-outline) " +
    "enabled:hover:bg-(--meal-surface)",
  todayBadge:
    "bg-(image:--meal-gradient) text-white " +
    "flex-none py-0.75 px-2.5 rounded-full text-xs font-[750]",
  addMeal: "min-h-13.5 px-6 shadow-[0_14px_28px_-14px_rgb(201_26_142_/_85%)]",
  calendar:
    "bg-(--meal-surface) shadow-[var(--meal-shadow)] " +
    "p-[clamp(12px,2.2vw,28px)] rounded-[clamp(26px,4vw,40px)]",
  slotCard:
    "bg-(--meal-surface) shadow-[var(--meal-shadow)] " +
    "flex min-w-0 flex-col gap-3.5 p-[clamp(16px,1.8vw,22px)] rounded-[32px]",
  weekDays: "grid grid-cols-7 gap-[clamp(4px,1vw,14px)]",
  day:
    "flex flex-col items-center min-w-0 min-h-21 gap-1.25 py-2.5 rounded-[clamp(11px,2.4vw,28px)] bg-(--meal-soft) " +
    "[&[aria-current=date]:not([aria-pressed=true])]:shadow-[inset_0_0_0_2px_#d91cab] " +
    "aria-pressed:bg-(image:--meal-gradient) aria-pressed:text-white aria-pressed:shadow-[0_16px_28px_-16px_rgb(201_26_142_/_90%)] " +
    "sm:min-h-29 sm:py-3.5 " +
    "group/meal-day",
  dayLabel:
    "text-(--meal-muted) text-[clamp(.68rem,1.2vw,.85rem)] font-[650] " +
    "group-aria-pressed/meal-day:text-white",
  dayNumber: "text-[clamp(1.4rem,2.6vw,2.1rem)] font-extrabold tabular-nums leading-none",
  dots: "flex gap-[clamp(1px,.35vw,4px)] mt-auto",
  dot:
    "w-[clamp(4px,.65vw,7px)] aspect-square rounded-full bg-(--meal-outline) " +
    "data-[planned=true]:bg-(--slot-color) " +
    "group-aria-pressed/meal-day:data-[planned=false]:bg-white/40 " +
    "group-aria-pressed/meal-day:data-[planned=true]:bg-white",
  slotIcon:
    "grid size-13 flex-none place-items-center rounded-[18px] bg-(--slot-color)",
  slotGrid:
    "grid gap-[clamp(12px,1.6vw,22px)] mt-[clamp(14px,2vw,22px)] " +
    "sm:grid-cols-2 " +
    "min-[1180px]:grid-cols-4",
  removeMeal:
    "enabled:hover:bg-(--meal-surface) " +
    "grid flex-none place-items-center rounded-full " +
    "w-11 h-11 text-(--meal-muted)",
  mealRow: "flex items-center gap-0.5 p-1 rounded-[20px] bg-(--meal-soft)",
  mealToggle:
    "flex min-w-0 min-h-11 flex-1 items-center gap-2 p-1 rounded-[16px] text-start " +
    "group/meal-toggle",
  check:
    "grid flex-none place-items-center rounded-full " +
    "w-8.5 h-8.5 border-2 border-(--meal-outline) " +
    "group-aria-checked/meal-toggle:border-transparent " +
    "group-aria-checked/meal-toggle:[background:var(--meal-gradient)_border-box] " +
    "group-aria-checked/meal-toggle:text-white",
  emptyState: "col-span-full py-4 px-0 text-(--meal-muted)",
  recipeResults:
    "max-h-60 overflow-y-auto overscroll-contain border border-divider rounded-[0.75rem] " +
    "[&_li_+_li]:border-t [&_li_+_li]:border-divider",
  recipeResult:
    "flex items-center justify-between gap-3 w-full min-h-12 py-2.5 px-3 cursor-pointer text-start " +
    "hover:[background:var(--secondary)] " +
    "focus-visible:[background:var(--secondary)]",
};
