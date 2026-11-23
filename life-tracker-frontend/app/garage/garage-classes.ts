export const garageStyles = {
  page:
    "[--garage-surface:#fff] [--garage-ink:#191630] [--garage-muted:#5e5875] [--garage-link:#263bc5] " +
    "[--garage-badge:#eeedf8] [--garage-outline:#aaa4c0] [--garage-card-shadow:0_12px_32px_#273cb019] " +
    "[--garage-banner-shadow:0_14px_32px_#263bd638] " +
    "dark:[--garage-surface:#191634] dark:[--garage-ink:#f2efff] dark:[--garage-muted:#b7b0ce] " +
    "dark:[--garage-link:#bfc8ff] dark:[--garage-badge:#302d49] dark:[--garage-outline:#77728d] " +
    "dark:[--garage-card-shadow:0_14px_32px_#05030f59] dark:[--garage-banner-shadow:0_14px_38px_#293ee049]",
  banner: "p-[clamp(1.5rem,3.5vw,3rem)] rounded-[clamp(1.75rem,4.2vw,3.75rem)] [background:linear-gradient(120deg,#243bd8,#0d81b4)] shadow-[var(--garage-banner-shadow)]",
  title: "text-[clamp(3.5rem,7vw,6.25rem)] font-black tracking-[-0.085em] leading-[0.98]",
  gauge:
    "hidden absolute inset-bs-[50%] end-[-1.5%] w-[min(38%,30rem)] h-auto overflow-visible -translate-y-1/2 " +
    "sm:block",
  gaugeRipple:
    "hidden " +
    "motion-safe:inline motion-safe:transform-fill motion-safe:origin-center motion-safe:animate-gauge-ripple",
  addVehicle:
    "min-h-[clamp(2.75rem,4vw,4.25rem)]! px-[clamp(1.125rem,2.1vw,2rem)]! rounded-full! bg-white! text-[#263bd1]! " +
    "font-[750]! text-[clamp(0.875rem,1.35vw,1.25rem)]! " +
    "hover:bg-[#eef1ff]! " +
    "focus-visible:outline-white",
  vehicleCard:
    "rounded-[clamp(1.5rem,3.25vw,2.75rem)] bg-(--garage-surface) text-(--garage-ink) shadow-[var(--garage-card-shadow)] " +
    "[&_:is(a,_button):focus-visible]:outline-2 [&_:is(a,_button):focus-visible]:outline-(--garage-link) " +
    "[&_:is(a,_button):focus-visible]:outline-offset-[3px]",
  muted: "text-(--garage-muted)",
  vehicleIcon:
    "rounded-[32%] [background:linear-gradient(135deg,#552bd9,#293cdb)] " +
    "data-[kind=motorcycle]:[background:linear-gradient(135deg,#253bdb,#0f7eaf)] " +
    "[&:is([data-kind=moped],_[data-kind=scooter])]:[background:linear-gradient(135deg,#0e80b0,#0c8c83)]",
  badge: "inline-flex items-center gap-1.5 rounded-full py-1.5 px-3 bg-(--garage-badge) text-sm font-[650] leading-[1.25]",
  quickAction: "min-w-0! min-h-[clamp(2.75rem,4vw,3.75rem)]! gap-1.5! py-2! px-1.5! rounded-full! font-[750]! leading-[1.15]",
  addFuel:
    "[background:linear-gradient(115deg,#323acb,#aa347e)]! text-white! " +
    "hover:[background:linear-gradient(115deg,#2c32b5,#96296d)]! " +
    "dark:[background:#f2efff]! dark:text-[#161229]! " +
    "dark:hover:[background:#e0dbf6]!",
  markAirFill:
    "border-2 border-(--garage-outline) bg-transparent! text-(--garage-ink)! " +
    "hover:bg-(--garage-badge)!",
};
