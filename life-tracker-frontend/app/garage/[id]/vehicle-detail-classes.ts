const pill = "inline-flex min-h-11 items-center justify-center gap-2 rounded-full px-4 py-1.5 text-sm font-bold";
const row = "grid-cols-(--history-columns) items-center gap-3 px-garage-detail wrap-anywhere";
const label = "text-caption font-bold tracking-wider uppercase";

export const maintenanceField = "mt-1.5 min-h-11 w-full min-w-0 rounded-control bg-secondary px-3 py-2 text-base sm:text-sm";

export const detail = {
  section: "grid min-w-0 gap-5",
  sectionHeader: "flex flex-wrap items-center justify-between gap-3 border-b border-divider pb-5",
  sectionTitle: "flex items-center gap-2.5 text-garage-section-title font-extrabold tracking-tight [&>svg]:shrink-0 [&>svg]:text-action",
  subheading: "text-section font-extrabold tracking-tight",
  specLabel: `${label} text-white/90`,
  label: `${label} text-muted-foreground`,
  card: "p-garage-detail shadow-card",
  empty: "rounded-card border border-dashed border-divider p-garage-detail text-center text-muted-foreground",
  tiles: "grid gap-5 sm:grid-cols-2 lg:grid-cols-3",
  metric: "text-garage-metric font-extrabold tracking-tight wrap-anywhere",
  metricUnit: "ms-1 text-xs font-semibold tracking-normal whitespace-nowrap text-muted-foreground",
  field: "min-h-12 w-full border border-divider text-foreground",
  primaryLink: `${pill} [background:var(--primary-fill)] text-primary-foreground`,
  secondaryLink: `${pill} bg-chip text-foreground hover:bg-muted`,
  iconLink: "grid size-11 flex-none place-items-center rounded-full text-muted-foreground hover:bg-chip hover:text-foreground",
  tab: "grid min-h-10 shrink-0 place-items-center rounded-full px-3 text-sm font-bold whitespace-nowrap text-muted-foreground hover:bg-chip aria-[current=page]:bg-foreground aria-[current=page]:text-background sm:px-4",
  recordList: "overflow-hidden shadow-card [&>article+article]:border-t [&>article+article]:border-divider [&>ul>li+li]:border-t [&>ul>li+li]:border-divider",
  rowHeader: `${row} ${label} hidden min-h-11 bg-muted text-muted-foreground sheet:grid`,
  row: `${row} grid min-h-17 py-3 max-sheet:gap-2.5`,
  stackedRow: "max-sheet:grid-cols-2",
  stackAcross: "max-sheet:col-span-full",
  cellLabel: `${label} mb-0.5 block text-muted-foreground sheet:sr-only`,
  rowActions: "flex min-w-(--history-actions) justify-end",
} as const;
