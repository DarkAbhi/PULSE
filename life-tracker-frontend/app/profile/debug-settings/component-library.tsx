"use client";

import { useId, useState, type ReactNode } from "react";
import { Plus, Settings2, Trash2, UserRound } from "lucide-react";
import PulseAppBar, { appBarControl, PulseAppBarBell } from "@/app/components/design-system/pulse-app-bar";
import PulseButton, { type PulseButtonVariant } from "@/app/components/design-system/pulse-button";
import PulseCard from "@/app/components/design-system/pulse-card";
import PulseChoiceGroup, { PulseChoiceControl } from "@/app/components/design-system/pulse-choice-group";
import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import PulseSelect from "@/app/components/design-system/pulse-select";
import PulseSwitch from "@/app/components/design-system/pulse-switch";
import PulseTimeInput from "@/app/components/design-system/pulse-time-input";

const buttonLabels = {
  primary: "Primary",
  secondary: "Secondary",
  tertiary: "Tertiary",
  gradient: "Gradient",
  soft: "Soft",
  chip: "Chip",
  outline: "Outline",
  warning: "Warning",
  destructive: "Destructive",
  outlineDanger: "Outline danger",
  filledDanger: "Filled danger",
  onHero: "On hero",
  menuItem: "Menu item",
  icon: "Icon",
  iconChip: "Icon chip",
  iconChipDanger: "Icon chip danger",
  iconOverlay: "Icon overlay",
  iconSecondary: "Icon secondary",
  iconDanger: "Icon danger",
} satisfies Record<PulseButtonVariant, string>;

const buttonVariants = Object.keys(buttonLabels) as PulseButtonVariant[];
const textButtonVariants = buttonVariants.filter(variant => !variant.startsWith("icon") && variant !== "onHero" && variant !== "menuItem");
const iconButtonVariants = buttonVariants.filter(variant => variant.startsWith("icon"));

const workoutOptions = [
  { value: "strength", label: "Strength" },
  { value: "cardio", label: "Cardio" },
  { value: "mobility", label: "Mobility" },
];

function PreviewSection({ title, children }: { title: string; children: ReactNode }) {
  const headingId = useId();

  return (
    <PulseCard as="section" aria-labelledby={headingId} className="grid content-start gap-4 p-5 shadow-card sm:p-6">
      <h3 id={headingId} className="text-lg font-bold">{title}</h3>
      {children}
    </PulseCard>
  );
}

function ButtonsPreview() {
  return (
    <PreviewSection title="Buttons">
      <div className="flex flex-wrap items-center gap-2.5">
        {textButtonVariants.map(variant => (
          <PulseButton
            key={variant}
            type="button"
            variant={variant}
            className={variant === "gradient" ? "[--button-fill:var(--primary-gradient)] [--button-foreground:white]" : undefined}
          >
            {buttonLabels[variant]}
          </PulseButton>
        ))}
        <PulseButton type="button" shape="pill" icon={<Plus className="size-4" aria-hidden="true" />}>Add workout</PulseButton>
        <PulseButton type="button" disabled>Disabled</PulseButton>
      </div>
      <div className="flex flex-wrap items-center gap-2.5">
        <div className="rounded-xl bg-(image:--primary-gradient) p-1.5">
          <PulseButton type="button" variant="onHero">{buttonLabels.onHero}</PulseButton>
        </div>
        <div className="min-w-32 rounded-xl border border-divider bg-card p-1.5">
          <PulseButton type="button" variant="menuItem">{buttonLabels.menuItem}</PulseButton>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {iconButtonVariants.map(variant => {
          const Icon = variant === "iconDanger" || variant === "iconChipDanger" ? Trash2 : Plus;

          return (
            <div key={variant} className="grid justify-items-center gap-1.5 text-center">
              <div className={variant === "iconOverlay" ? "rounded-lg bg-(image:--primary-gradient) text-white" : undefined}>
                <PulseButton type="button" variant={variant} aria-label={buttonLabels[variant]}>
                  <Icon className="size-4" aria-hidden="true" />
                </PulseButton>
              </div>
              <span className="text-xs text-muted-foreground">{buttonLabels[variant]}</span>
            </div>
          );
        })}
      </div>
    </PreviewSection>
  );
}

function InputsPreview() {
  const selectId = useId();
  const [tracking, setTracking] = useState(true);
  const [time, setTime] = useState("17:30");

  return (
    <PreviewSection title="Inputs">
      <div className="grid gap-2">
        <h4 className="text-sm font-semibold">Dropdown</h4>
        <label htmlFor={selectId} className="font-medium">Workout type</label>
        <PulseSelect id={selectId} defaultValue="strength" className="min-h-12 w-full rounded-lg border border-divider bg-background px-3 py-2">
          {workoutOptions.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
        </PulseSelect>
      </div>
      <div className="grid gap-2">
        <h4 className="text-sm font-semibold">Time input</h4>
        <PulseTimeInput label="Workout reminder" value={time} onChange={setTime} />
      </div>
      <div className="grid gap-2">
        <h4 className="text-sm font-semibold">Switch</h4>
        <PulseSwitch checked={tracking} onChange={setTracking}>Track workouts</PulseSwitch>
      </div>
    </PreviewSection>
  );
}

function SelectionPreview() {
  const [primary, setPrimary] = useState("strength");
  const [focus, setFocus] = useState(["mobility"]);
  const [favorite, setFavorite] = useState(false);

  return (
    <PreviewSection title="Selection">
      <div className="grid gap-2">
        <h4 className="text-sm font-semibold">Radio buttons</h4>
        <PulseChoiceGroup label="Primary workout" options={workoutOptions} value={primary} onChange={setPrimary} columns />
      </div>
      <div className="grid gap-2">
        <h4 className="text-sm font-semibold">Checkboxes</h4>
        <PulseChoiceGroup label="Focus areas" options={workoutOptions} multiple value={focus} onChange={setFocus} columns />
      </div>
      <div className="grid gap-2">
        <h4 className="text-sm font-semibold">Selectable chip</h4>
        <PulseChoiceControl type="checkbox" name="sample-favorite" value="workout" checked={favorite} onCheckedChange={setFavorite} className="inline-flex justify-self-start">
          <span className="rounded-full border border-divider px-4 py-2 text-sm font-semibold peer-checked:border-action peer-checked:bg-accent peer-focus-visible:outline-2 peer-focus-visible:outline-ring">Favorite workout</span>
        </PulseChoiceControl>
      </div>
    </PreviewSection>
  );
}

function DialogsPreview() {
  const titleId = useId();
  const [dialog, setDialog] = useState<"settings" | "confirmation" | null>(null);
  const [action, setAction] = useState("");
  const close = () => setDialog(null);

  return (
    <PreviewSection title="Dialogs">
      <div className="flex flex-wrap gap-2.5">
        <PulseButton type="button" onClick={() => setDialog("settings")}>Open dialog</PulseButton>
        <PulseButton type="button" variant="destructive" onClick={() => setDialog("confirmation")}>Open confirmation</PulseButton>
      </div>
      <p role="status" className="text-sm text-muted-foreground">{action}</p>
      {dialog === "settings" && (
        <PulseDialog labelledBy={titleId} onDismiss={close}>
          <PulseDialogHeader id={titleId} description="Preview a dialog with a title, close button, and footer actions." icon={<Settings2 className="size-5" />} onClose={close}>Workout settings</PulseDialogHeader>
          <PulseDialogActions>
            <PulseDialogAction type="button" variant="secondary" onClick={close}>Cancel</PulseDialogAction>
            <PulseDialogAction type="button" onClick={() => { setAction("Sample settings saved."); close(); }}>Save</PulseDialogAction>
          </PulseDialogActions>
        </PulseDialog>
      )}
      <PulseConfirmationDialog
        isOpen={dialog === "confirmation"}
        title="Delete sample workout?"
        description="This only affects the preview. No account data will change."
        confirmText="Delete"
        onClose={close}
        onConfirm={() => { setAction("Sample deletion confirmed."); close(); }}
      />
    </PreviewSection>
  );
}

function AppBarPreview() {
  const previewAction = () => {};

  return (
    <PreviewSection title="App bar">
      {[
        { label: "No unread notifications", hasUnread: false },
        { label: "Unread notifications", hasUnread: true },
      ].map(({ label, hasUnread }) => (
        <div key={label} className="grid gap-2">
          <h4 className="text-sm font-semibold">{label}</h4>
          <div className="rounded-xl bg-background p-3">
            <PulseAppBar
              back={{ onClick: previewAction }}
              backLabel="Back"
              profileControl={(
                <button type="button" aria-label="Profile" className={`${appBarControl} cursor-pointer`}>
                  <UserRound className="size-5" aria-hidden="true" />
                </button>
              )}
              notifications={(
                <button type="button" aria-label={hasUnread ? "Notifications, unread" : "Notifications"} className={`${appBarControl} cursor-pointer`}>
                  <PulseAppBarBell hasUnread={hasUnread} />
                </button>
              )}
            />
          </div>
        </div>
      ))}
    </PreviewSection>
  );
}

export default function ComponentLibrary() {
  return (
    <section aria-labelledby="component-library" className="grid gap-5">
      <h2 id="component-library" className="text-xl font-bold">Component library</h2>
      <div className="grid gap-4 md:grid-cols-2 [&>*]:min-w-0">
        <ButtonsPreview />
        <InputsPreview />
        <SelectionPreview />
        <div className="grid content-start gap-4">
          <PreviewSection title="Cards">
            <div className="rounded-xl bg-background p-3">
              <PulseCard as="article" className="p-4">
                <h4 className="font-semibold">Today’s workout</h4>
                <p className="text-sm text-muted-foreground">Upper body · 6 exercises</p>
              </PulseCard>
            </div>
          </PreviewSection>
          <DialogsPreview />
          <AppBarPreview />
        </div>
      </div>
    </section>
  );
}
