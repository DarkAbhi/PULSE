export const vehicleDetailPalette = [
  "bg-[#f6f7ff] dark:bg-[#0d0a1f] text-(--detail-ink)",
  "[--detail-surface:#fff] dark:[--detail-surface:#1a1636]",
  "[--detail-inset:#f1f2fb] dark:[--detail-inset:#231e45]",
  "[--detail-ink:#1a1740] dark:[--detail-ink:#f4f1ff]",
  "[--detail-muted:#5e5a7d] dark:[--detail-muted:#a49ec4]",
  "[--detail-line:rgb(26_23_64/12%)] dark:[--detail-line:rgb(244_241_255/14%)]",
  "[--detail-chip:rgb(26_23_64/7%)] dark:[--detail-chip:rgb(244_241_255/10%)]",
  "[--detail-accent:#4c6bff]",
  "[--detail-danger:#b82b22] dark:[--detail-danger:#ff7a6b]",
  "[--detail-warning:#985c00] dark:[--detail-warning:#ffc83d]",
  "[--detail-shadow:0_1px_2px_rgb(26_23_64/6%),0_14px_32px_-20px_rgb(76_107_255/40%)]",
  "dark:[--detail-shadow:0_1px_2px_rgb(0_0_0/30%),0_14px_32px_-22px_rgb(0_0_0/80%)]",
  "[--detail-primary:linear-gradient(90deg,#4c6bff,#b22bd8)] dark:[--detail-primary:#f4f1ff]",
  "[--detail-primary-ink:#fff] dark:[--detail-primary-ink:#0d0a1f]",
  "[--detail-pad:clamp(16px,2.5vw,24px)]",
].join(" ");

const focus = "focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-(--detail-accent)";
const pill = `inline-flex items-center justify-center gap-2 min-h-11! rounded-full! px-[18px]! py-1.5! text-center text-[0.95rem]! font-bold! ${focus}`;
const icon = `grid! size-11 place-items-center flex-none rounded-full! text-(--detail-muted)! hover:bg-(--detail-chip)! ${focus}`;
const surface = "min-w-0 rounded-[28px] bg-(--detail-surface) shadow-(--detail-shadow)";
const label = "text-[0.75rem] font-bold tracking-[0.06em] uppercase";
const row = "items-center grid-cols-(--history-columns) gap-3 px-(--detail-pad) wrap-anywhere";

export const detail = {
  muted: "text-(--detail-muted)",
  danger: "text-(--detail-danger)",
  warning: "text-(--detail-warning)",
  section: "grid min-w-0 gap-5",
  sectionHeader: "flex flex-wrap items-center justify-between gap-3 border-b border-(--detail-line) pb-5",
  sectionTitle: "flex items-center gap-2.5 text-[clamp(1.3rem,2.2vw,1.65rem)] leading-[1.2] font-extrabold tracking-[-0.03em] [&>svg]:shrink-0 [&>svg]:text-(--detail-accent)",
  subheading: "text-[1.15rem] font-extrabold tracking-[-0.02em]",
  specLabel: `${label} text-white/90`,
  label: `${label} text-(--detail-muted)`,
  card: `${surface} p-(--detail-pad)`,
  empty: "rounded-[28px] border-[1.5px] border-dashed border-(--detail-line) p-(--detail-pad) text-center text-(--detail-muted)",
  tiles: "grid grid-cols-3 gap-5 max-[1001px]:grid-cols-2 max-[641px]:grid-cols-1",
  metric: "text-[clamp(1.5rem,2.4vw,2.1rem)] leading-[1.1] font-extrabold tracking-[-0.035em] wrap-anywhere",
  metricUnit: "ms-1 text-[0.5em] font-semibold tracking-normal whitespace-nowrap text-(--detail-muted)",
  field: `min-h-12 w-full border-[1.5px] border-(--detail-line) text-(--detail-ink) ${focus}`,
  heroButton: `${pill} bg-white! text-[#1d3bd9]! hover:bg-[#eef1ff]!`,
  primaryButton: `${pill} [background:var(--detail-primary)]! hover:[background:var(--detail-primary)]! text-(--detail-primary-ink)!`,
  secondaryButton: `${pill} bg-(--detail-chip)! text-(--detail-ink)! hover:bg-(--detail-line)!`,
  dangerButton: `${pill} border-[1.5px] border-current bg-transparent! text-(--detail-danger)! hover:bg-(--detail-danger)/12!`,
  iconButton: `${icon} hover:text-(--detail-ink)!`,
  deleteIcon: `${icon} hover:text-(--detail-danger)!`,
  tab: `grid min-h-10 place-items-center rounded-full px-[18px] text-[0.92rem] font-bold whitespace-nowrap text-(--detail-muted) hover:bg-(--detail-chip) aria-[current=page]:bg-(--detail-ink) aria-[current=page]:text-[#f6f7ff] dark:aria-[current=page]:text-[#0d0a1f] max-[641px]:px-[clamp(6px,2vw,12px)] max-[641px]:text-[clamp(0.8rem,3.5vw,0.92rem)] ${focus}`,
  recordList: `${surface} overflow-hidden [&>article+article]:border-t [&>article+article]:border-(--detail-line) [&>ul>li+li]:border-t [&>ul>li+li]:border-(--detail-line)`,
  rowHeader: `${row} ${label} hidden min-h-11 bg-(--detail-inset) text-(--detail-muted) min-[761px]:grid`,
  row: `${row} grid min-h-[68px] py-3 max-[761px]:gap-2.5`,
  stackedRow: "max-[761px]:grid-cols-2",
  stackAcross: "max-[761px]:col-span-full",
  cellLabel: `${label} mb-0.5 block text-(--detail-muted) min-[761px]:sr-only`,
  rowActions: "flex min-w-(--history-actions) justify-end",
} as const;
