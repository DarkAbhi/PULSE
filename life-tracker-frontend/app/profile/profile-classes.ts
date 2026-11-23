export const profilePalette = [
  "[--profile-surface:#fff] dark:[--profile-surface:#1a1636]",
  "[--profile-inset:#f1f2fb] dark:[--profile-inset:#231e45]",
  "[--profile-ink:#1a1740] dark:[--profile-ink:#f4f1ff]",
  "[--profile-muted:#5e5a7d] dark:[--profile-muted:#a49ec4]",
  "[--profile-line:rgb(26_23_64/12%)] dark:[--profile-line:rgb(244_241_255/14%)]",
  "[--profile-chip:rgb(26_23_64/7%)] dark:[--profile-chip:rgb(244_241_255/10%)]",
  "[--profile-shadow:0_1px_2px_rgb(26_23_64/6%),0_14px_32px_-20px_rgb(76_107_255/40%)]",
  "dark:[--profile-shadow:0_1px_2px_rgb(0_0_0/30%),0_14px_32px_-22px_rgb(0_0_0/80%)]",
  "[--profile-accent:#4c6bff] dark:[--profile-accent:#8fa2ff]",
  "[--profile-primary:linear-gradient(90deg,#4c6bff,#b22bd8)] dark:[--profile-primary:#f4f1ff]",
  "[--profile-primary-ink:#fff] dark:[--profile-primary-ink:#0d0a1f]",
  "[--profile-danger:#c93028] dark:[--profile-danger:#ff7a6b]",
].join(" ");

export const sectionClass = "flex min-w-0 flex-col gap-5";
export const sectionHeadingClass = "flex items-center gap-2.5 text-[clamp(1.3rem,2.2vw,1.65rem)] leading-[1.2] font-extrabold tracking-[-0.03em] [&_svg]:size-[26px] [&_svg]:shrink-0 [&_svg]:text-(--profile-accent)";
export const sectionDescriptionClass = "mt-1 max-w-[62ch] text-[0.95rem] text-(--profile-muted)";
export const cardClass = "rounded-[28px] bg-(--profile-surface) shadow-(--profile-shadow)";

export const settingRowClass = "grid min-h-[84px] grid-cols-[44px_minmax(0,1fr)_auto] items-center gap-4 px-6 py-3 max-[761px]:grid-cols-[44px_minmax(0,1fr)]";
export const iconTileClass = "grid size-11 shrink-0 place-items-center rounded-[14px] bg-(--profile-accent)/14 text-(--profile-accent) [&_svg]:size-5";
export const labelClass = "text-[0.72rem] font-bold tracking-[0.05em] text-(--profile-muted) uppercase";
export const settingValueClass = "mt-0.5 text-[1.05rem] font-extrabold wrap-anywhere";

export const pillClass = "min-h-11! rounded-full! px-[18px]! py-0! text-[0.95rem]! font-bold! whitespace-nowrap";
export const settingButtonClass = `${pillClass} bg-(--profile-chip)! text-(--profile-ink)! hover:bg-(--profile-line)! max-[761px]:col-span-full`;
