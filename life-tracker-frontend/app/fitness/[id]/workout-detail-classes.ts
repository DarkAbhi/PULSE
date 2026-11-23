export const workoutPalette = [
  "[--workout-card:#fff] dark:[--workout-card:#1a1636]",
  "[--workout-inset:#f1f2fb] dark:[--workout-inset:#231e45]",
  "[--workout-ink:#1a1740] dark:[--workout-ink:#f4f1ff]",
  "[--workout-muted:#5e5a7d] dark:[--workout-muted:#a49ec4]",
  "[--workout-line:rgb(26_23_64/12%)] dark:[--workout-line:rgb(244_241_255/14%)]",
  "[--workout-chip:rgb(26_23_64/7%)] dark:[--workout-chip:rgb(244_241_255/10%)]",
  "[--workout-danger:#d6342a] dark:[--workout-danger:#ff7a6b]",
  "[--workout-shadow:0_1px_2px_rgb(26_23_64/6%),0_14px_32px_-20px_rgb(76_107_255/40%)]",
  "dark:[--workout-shadow:0_1px_2px_rgb(0_0_0/30%),0_14px_32px_-22px_rgb(0_0_0/80%)]",
  "[--workout-primary:linear-gradient(90deg,#4c6bff,#b22bd8)] dark:[--workout-primary:#f4f1ff]",
  "[--workout-primary-ink:#fff] dark:[--workout-primary-ink:#0d0a1f]",
  "[--workout-accent:#4c6bff]",
].join(" ");

export const workoutLabelClass = "text-[0.7rem] font-bold tracking-[0.05em] text-(--workout-muted) uppercase";
export const workoutChipClass = "inline-flex items-center rounded-full bg-(--workout-chip) px-2.5 py-1 text-[0.8rem] font-semibold";
export const setNumberClass = "grid size-[30px] place-items-center rounded-full bg-(--workout-chip) text-[0.85rem] font-extrabold";

export const workoutFocusClass = "focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-(--workout-accent)";
const pillClass = `min-h-11! rounded-full! px-[18px]! py-0! text-[0.95rem]! font-bold! whitespace-nowrap ${workoutFocusClass}`;
export const workoutPrimaryClass = `${pillClass} [background:var(--workout-primary)]! text-(--workout-primary-ink)!`;
export const workoutSoftClass = `${pillClass} bg-(--workout-chip)! text-(--workout-ink)! hover:bg-(--workout-line)!`;
export const workoutDeleteClass = `${pillClass} border-[1.5px] border-(--workout-danger) bg-transparent! text-(--workout-danger)! hover:bg-(--workout-danger)/12!`;

const iconClass = `rounded-full! text-(--workout-muted)! hover:bg-(--workout-chip)! ${workoutFocusClass}`;
export const workoutIconClass = `${iconClass} hover:text-(--workout-ink)!`;
export const workoutDeleteIconClass = `${iconClass} hover:text-(--workout-danger)!`;
