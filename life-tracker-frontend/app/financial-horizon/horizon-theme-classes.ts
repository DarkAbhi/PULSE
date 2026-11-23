import { horizonPalette } from "@/app/components/design-system/horizon-palette";

export const horizonClasses = {
  page:
    horizonPalette + " " +
    "[--horizon-sun:#ffe08a] [--horizon-amber:#b26a00] [--horizon-budget-available:#0e8a66] [--horizon-budget-deficit:#d6342a] " +
    "dark:[--horizon-sun:#ffc83d] dark:[--horizon-amber:#ffc83d] dark:[--horizon-budget-available:#3ddca6] dark:[--horizon-budget-deficit:#ff7a6b] " +
    "[&_.pulse-card]:shadow-(--horizon-shadow) " +
    "forced-colors:[&_input[type=checkbox]:checked]:bg-none",
  tabs: "rounded-[28px] [background:var(--horizon-surface)] shadow-[var(--horizon-shadow)]",
  tab:
    "flex items-center gap-2 w-full min-w-0 min-h-11 py-2 px-3 rounded-full text-(--horizon-muted) text-[0.95rem] font-bold leading-[1.2] text-start wrap-anywhere " +
    "[&:not([aria-current])]:hover:[background:var(--horizon-chip)] [&:not([aria-current])]:hover:text-(--horizon-ink) " +
    "[&[aria-current]]:[background:var(--horizon-button)] [&[aria-current]]:text-primary-foreground " +
    "xl:justify-center xl:text-center " +
    "forced-colors:[&[aria-current]]:outline-1 forced-colors:[&[aria-current]]:outline-[ButtonText]",
  primary:
    "rounded-full! " +
    "min-h-11! gap-2! py-0! px-4.5! text-[0.95rem]! font-bold! whitespace-nowrap " +
    "[&_svg]:size-5 [&_svg]:flex-none " +
    "enabled:active:scale-95 " +
    "[background:var(--horizon-button)]! text-primary-foreground! " +
    "enabled:hover:brightness-[0.93] " +
    "motion-reduce:transition-none",
  segment:
    "rounded-full! " +
    "min-h-9! py-0! px-4! [background:transparent]! text-(--horizon-muted)! text-[0.9rem]! font-bold! " +
    "aria-[pressed=false]:hover:[background:var(--horizon-chip)]! " +
    "aria-pressed:[background:var(--card)]! aria-pressed:text-foreground! aria-pressed:shadow-[var(--horizon-shadow)] " +
    "forced-colors:aria-pressed:outline-1 forced-colors:aria-pressed:outline-[ButtonText]",
  softAction:
    "rounded-full! " +
    "min-h-11! gap-2! py-0! px-[var(--horizon-soft-action-padding,1.125rem)]! text-[0.95rem]! font-bold! whitespace-nowrap " +
    "[&_svg]:size-5 [&_svg]:flex-none " +
    "enabled:active:scale-95 " +
    "bg-[color:var(--horizon-soft-action-bg,var(--horizon-chip))]! text-[color:var(--horizon-soft-action-text,var(--horizon-ink))]! " +
    "enabled:hover:bg-[color:var(--horizon-soft-action-hover,var(--horizon-line))]! " +
    "motion-reduce:transition-none",
  outlineAction:
    "rounded-full! " +
    "min-h-11! gap-2! py-0! px-4.5! text-[0.95rem]! font-bold! whitespace-nowrap " +
    "[&_svg]:size-5 [&_svg]:flex-none " +
    "enabled:active:scale-95 " +
    "border-[1.5px] border-(--horizon-line) [background:transparent]! text-(--horizon-ink)! " +
    "enabled:hover:[background:var(--horizon-chip)]! enabled:hover:text-(--horizon-ink)! " +
    "motion-reduce:transition-none",
  dangerAction:
    "rounded-full! " +
    "min-h-11! gap-2! py-0! px-4.5! text-[0.95rem]! font-bold! whitespace-nowrap " +
    "[&_svg]:size-5 [&_svg]:flex-none " +
    "enabled:active:scale-95 " +
    "[background:color-mix(in_oklab,var(--destructive)_15%,transparent)]! text-destructive! " +
    "enabled:hover:text-destructive! " +
    "motion-reduce:transition-none",
  iconAction:
    "rounded-full! " +
    "[&_svg]:size-5 [&_svg]:flex-none " +
    "inline-grid! size-11! flex-none place-items-center bg-transparent! text-[color:var(--horizon-icon-text,var(--horizon-muted))]! " +
    "enabled:hover:bg-[color:var(--horizon-icon-hover-bg,var(--horizon-chip))]! enabled:hover:text-[color:var(--horizon-icon-hover,var(--horizon-ink))]!",
  dangerIcon:
    "rounded-full! " +
    "[&_svg]:size-5 [&_svg]:flex-none " +
    "inline-grid! w-11! h-11! flex-none place-items-center [background:transparent]! text-(--horizon-muted)! " +
    "enabled:hover:[background:var(--horizon-chip)]! enabled:hover:text-destructive!",
  sectionTitle: "text-[clamp(1.3rem,2.2vw,1.65rem)] font-extrabold leading-[1.25] tracking-[-0.03em]",
  empty:
    "p-8 border-[1.5px] border-dashed border-(--horizon-line) rounded-[28px] [background:transparent] " +
    "[&.pulse-card]:shadow-none!",
  switch:
    "relative flex-none w-12 h-7 rounded-full mt-2 [background:var(--horizon-line)] [transition:background_150ms] " +
    "aria-checked:[background:var(--horizon-gradient)] " +
    "after:absolute after:top-0.75 after:start-0.75 after:w-5.5 after:h-5.5 after:rounded-full after:[background:white] after:[transition:translate_150ms] " +
    "aria-checked:after:[translate:20px] " +
    "motion-reduce:transition-none " +
    "motion-reduce:after:transition-none " +
    "forced-colors:outline-1 forced-colors:outline-[ButtonText] " +
    "forced-colors:after:[background:ButtonText]",
  sectionHeader: "flex flex-wrap items-center justify-between gap-4 pb-5 border-b border-(--horizon-line)",
  sectionDescription: "max-w-[64ch] mt-1 text-(--horizon-muted) text-[0.95rem]",
  sectionIcon: "text-[#4c6bff]",
  iconTile: "grid w-11 h-11 flex-none place-items-center rounded-[14px] [background:rgb(76_107_255_/_15%)] text-[#4c6bff]",
  chip:
    "inline-flex items-center gap-1.5 py-1 px-2.5 rounded-full [background:var(--horizon-chip)] text-[0.8rem] font-semibold " +
    "data-[tone=dim]:text-(--horizon-muted) " +
    "data-[tone=good]:[background:color-mix(in_oklab,var(--success)_15%,transparent)] data-[tone=good]:text-success " +
    "data-[tone=bad]:[background:color-mix(in_oklab,var(--destructive)_15%,transparent)] data-[tone=bad]:text-destructive " +
    "data-[tone=warn]:[background:rgb(255_200_61_/_25%)] data-[tone=warn]:text-warning",
  badge:
    "inline-flex items-center gap-1.5 rounded-full bg-(--horizon-chip) px-3 py-1 text-[0.8rem] font-bold " +
    "data-[tone=warn]:bg-[#ffc83d]/30 data-[tone=warn]:text-warning",
  select:
    "w-full appearance-none border-[1.5px] border-(--horizon-line) rounded-full bg-(--horizon-surface) text-(--horizon-ink) py-0 ps-4 pe-10 text-[0.92rem] font-semibold",
  search:
    "h-11 min-w-0 border-[1.5px] border-(--horizon-line) rounded-full bg-(--horizon-surface) text-(--horizon-ink) " +
    "w-full py-0 ps-11 pe-4",
  summary:
    "grid grid-cols-3 gap-5 " +
    "[@media(max-width:1000px)]:grid-cols-1",
  summaryCard:
    "flex min-h-46 flex-col gap-3 p-6 " +
    "[@media(max-width:1000px)]:min-h-0",
  summaryHeader: "flex min-h-11 items-center gap-3",
  summaryIcon:
    "grid w-10 h-10 flex-none place-items-center rounded-[14px] [background:var(--horizon-chip)] " +
    "group-data-[pool=starting]/pool:[background:rgb(255_255_255_/_18%)] " +
    "group-data-[pool=available]/pool:[background:rgb(58_42_0_/_12%)]",
  summaryLabel:
    "text-xs font-bold tracking-[0.06em] leading-[1.2] uppercase " +
    "group-data-[pool=starting]/pool:text-[rgb(255_255_255_/_85%)]",
  summaryEditButton:
    "ms-auto " +
    "[--horizon-icon-hover-bg:rgb(255_255_255_/_18%)] [--horizon-icon-hover:white] [--horizon-icon-text:white]",
  summaryBadge:
    "ms-auto " +
    "whitespace-nowrap " +
    "data-[tone=warn]:text-(--horizon-amber) " +
    "group-data-[pool=available]/pool:[background:rgb(58_42_0_/_14%)]",
  summaryValue: "text-[clamp(1.9rem,3vw,2.5rem)] tabular-nums font-extrabold tracking-[-0.04em] leading-[1.1] wrap-anywhere",
  summaryHint:
    "min-h-[2.7em] text-[0.9rem] leading-[1.35] " +
    "group-data-[pool=starting]/pool:text-[rgb(255_255_255_/_85%)] " +
    "group-data-[pool=committed]/pool:text-(--horizon-muted) " +
    "group-data-[pool=available]/pool:opacity-85 " +
    "[@media(max-width:1000px)]:min-h-0",
  startingPool:
    "[background:linear-gradient(135deg,#2b1b8f,#5b2ce0_60%,#9b2bd8)] text-white " +
    "group/pool",
  availablePool:
    "[background:var(--horizon-sun)] text-[#3a2a00] " +
    "group/pool",
  summaryEditForm:
    "flex flex-wrap items-center gap-2 " +
    "[&_input]:h-11 [&_input]:min-w-0 [&_input]:[flex:1_1_6.5rem] [&_input]:px-3.5 [&_input]:border-2 [&_input]:border-[rgb(255_255_255_/_60%)] [&_input]:rounded-[14px] [&_input]:[background:rgb(255_255_255_/_15%)] [&_input]:text-white [&_input]:text-[1.2rem] [&_input]:font-extrabold",
  summaryEditAction:
    "[--horizon-soft-action-padding:0.875rem] [--horizon-soft-action-bg:rgb(255_255_255_/_20%)] [--horizon-soft-action-text:white] [--horizon-soft-action-hover:rgb(255_255_255_/_30%)]",
  projectionBar:
    "[background:var(--horizon-gradient)] " +
    "forced-colors:[background:Highlight] forced-colors:forced-color-adjust-none",
  budgetPeriod: "data-[tone=warn]:text-(--horizon-amber) data-[tone=warn]:whitespace-nowrap",
  budgets: "@container/budgets",
  budgetGrid:
    "grid grid-cols-[minmax(0,420px)] gap-5 " +
    "@min-[740px]/budgets:grid-cols-[repeat(2,minmax(0,420px))] " +
    "@min-[1120px]/budgets:grid-cols-[repeat(3,minmax(0,420px))]",
  budgetCard: "@container grid grid-rows-[auto_1fr_auto] gap-4 p-6",
  budgetHeader:
    "flex min-h-11 items-start gap-3 " +
    "@max-[300px]:grid @max-[300px]:grid-cols-[auto_minmax(0,1fr)] @max-[300px]:gap-y-1",
  budgetIcon: "[background:rgb(76_107_255_/_14%)]!",
  budgetTitle: "text-[1.1rem] font-extrabold tracking-[-0.02em] leading-[1.25] wrap-anywhere",
  budgetLinks:
    "mt-0.5 text-(--horizon-muted) text-[0.88rem] wrap-anywhere " +
    "[&_summary]:w-fit [&_summary]:cursor-pointer",
  budgetActions:
    "flex flex-none gap-2 " +
    "@max-[300px]:col-start-2 @max-[300px]:justify-self-end",
  budgetAmounts:
    "grid grid-cols-2 rounded-[18px] [background:var(--horizon-inset)] " +
    "[&_>_div]:py-3.5 [&_>_div]:px-[clamp(8px,3.5cqi,16px)] " +
    "[&_>_div_+_div]:border-s [&_>_div_+_div]:border-(--horizon-line) " +
    "[&_dt]:text-(--horizon-muted) [&_dt]:text-[0.72rem] [&_dt]:font-bold [&_dt]:tracking-[0.05em] [&_dt]:uppercase " +
    "[&_dd]:[margin-block-start:2px] [&_dd]:text-[1.15rem] [&_dd]:font-extrabold [&_dd]:wrap-anywhere " +
    "tabular-nums",
  budgetBalance:
    "flex flex-wrap items-baseline justify-between gap-[4px_12px] text-(--horizon-muted) text-[0.92rem] " +
    "[&_b]:font-extrabold [&_b]:wrap-anywhere " +
    "[&_b]:tabular-nums",
  budgetPercent:
    "tabular-nums " +
    "mt-1.5 text-(--horizon-muted) text-[0.82rem] text-end",
  budgetUsed: "text-(--horizon-amber)",
  budgetAvailable: "text-(--horizon-budget-available)",
  budgetDeficit: "text-(--horizon-budget-deficit)",
  budgetTrack:
    "h-2.5 mt-2.5 overflow-hidden rounded-full [background:var(--horizon-chip)] " +
    "[&_>_div]:h-full [&_>_div]:rounded-[inherit]",
  budgetOver:
    "[background:var(--horizon-budget-deficit)] " +
    "forced-colors:[background:Highlight] forced-colors:forced-color-adjust-none",
  budgetLow:
    "[background:linear-gradient(90deg,#19d3f3,#4c6bff)] " +
    "forced-colors:[background:Highlight] forced-colors:forced-color-adjust-none",
  budgetMedium:
    "[background:linear-gradient(90deg,#ffc83d,#f0a020)] " +
    "forced-colors:[background:Highlight] forced-colors:forced-color-adjust-none",
  budgetHigh:
    "[background:linear-gradient(90deg,#ff8a3d,#ff2fd0)] " +
    "forced-colors:[background:Highlight] forced-colors:forced-color-adjust-none",
  committedPool: "group/pool",
};
