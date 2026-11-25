"use client";

import { useId, useState, type ReactNode } from "react";
import { ChevronDown, Plus, Settings2, Trash2, UserRound } from "lucide-react";
import PulseAppBar, { appBarControl, PulseAppBarBell } from "@/app/components/design-system/pulse-app-bar";
import PulseButton, { type PulseButtonVariant } from "@/app/components/design-system/pulse-button";
import PulseIconButton, { type PulseIconButtonVariant } from "@/app/components/design-system/pulse-icon-button";
import PulseDropdown, { PulseDropdownItem, PulseDropdownSeparator } from "@/app/components/design-system/pulse-dropdown";
import PulseCard from "@/app/components/design-system/pulse-card";
import PulseChoiceGroup, { PulseChoiceControl } from "@/app/components/design-system/pulse-choice-group";
import PulseConfirmationDialog from "@/app/components/design-system/pulse-confirmation-dialog";
import PulseDialog, { PulseDialogAction, PulseDialogActions, PulseDialogHeader } from "@/app/components/design-system/pulse-dialog";
import PulseSelect from "@/app/components/design-system/pulse-select";
import PulseSwitch from "@/app/components/design-system/pulse-switch";
import PulseTimeInput from "@/app/components/design-system/pulse-time-input";
import PulseTabs from "@/app/components/design-system/pulse-tabs";

const buttonLabels = {
  primary: "Primary",
  secondary: "Secondary",
  outline: "Outline",
  ghost: "Ghost",
  destructive: "Destructive",
  link: "Link",
} satisfies Record<PulseButtonVariant, string>;

const iconButtonLabels = {
  plain: "Plain",
  secondary: "Secondary",
  overlay: "Overlay",
  danger: "Danger",
} satisfies Record<PulseIconButtonVariant, string>;

const textButtonVariants = Object.keys(buttonLabels) as PulseButtonVariant[];
const iconButtonVariants = Object.keys(iconButtonLabels) as PulseIconButtonVariant[];

const workoutOptions = [
  { value: "strength", label: "Strength" },
  { value: "cardio", label: "Cardio" },
  { value: "mobility", label: "Mobility" },
];

function PreviewSection({ title, children }: { title: string; children: ReactNode }) {
  const headingId = useId();

  return (
    <PulseCard as="section" aria-labelledby={headingId} padding="md" className="grid content-start gap-4">
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
          >
            {buttonLabels[variant]}
          </PulseButton>
        ))}
        <PulseButton type="button" icon={<Plus className="size-4" aria-hidden="true" />}>Add workout</PulseButton>
        <PulseButton type="button" disabled>Disabled</PulseButton>
      </div>
      <div className="flex flex-wrap items-center gap-2.5">
        <div className="rounded-xl bg-(image:--primary-gradient) p-1.5">
          <PulseButton type="button" variant="secondary" className="[--btn-cancel-bg:white] [--btn-cancel-text:var(--on-hero-action)] [--btn-cancel-hover:var(--on-hero-hover)]">On hero</PulseButton>
        </div>
        <PulseDropdown trigger={({ isOpen, ...triggerProps }) => (
          <PulseButton {...triggerProps} type="button" variant="secondary" icon={<ChevronDown className={`size-4 transition-transform motion-reduce:transition-none ${isOpen ? "rotate-180" : ""}`} aria-hidden="true" />} iconPosition="end">
            Dropdown
          </PulseButton>
        )}>
          <PulseDropdownItem icon={<Plus className="size-4" aria-hidden="true" />} description="Shows a description line" onSelect={() => {}}>Menu item</PulseDropdownItem>
          <PulseDropdownSeparator />
          <PulseDropdownItem icon={<Settings2 className="size-4" aria-hidden="true" />} onSelect={() => {}}>Another item</PulseDropdownItem>
        </PulseDropdown>
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {iconButtonVariants.map(variant => {
          const Icon = variant === "danger" ? Trash2 : Plus;

          return (
            <div key={variant} className="grid justify-items-center gap-1.5 text-center">
              <div className={variant === "overlay" ? "rounded-full bg-(image:--primary-gradient) text-white" : undefined}>
                <PulseIconButton type="button" variant={variant} aria-label={iconButtonLabels[variant]}>
                  <Icon className="size-4" aria-hidden="true" />
                </PulseIconButton>
              </div>
              <span className="text-xs text-muted-foreground">{iconButtonLabels[variant]}</span>
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

const sectionTabs = [
  { value: "today", label: "Today" },
  { value: "history", label: "History" },
  { value: "goals", label: "Goals" },
];

const typeTabs = [
  { value: "all", label: "All" },
  { value: "debit", label: "Debit" },
  { value: "credit", label: "Credit" },
];

function TabsPreview() {
  const [section, setSection] = useState("today");
  const [type, setType] = useState("all");

  return (
    <PreviewSection title="Tabs">
      <div className="grid gap-2">
        <h4 className="text-sm font-semibold">Primary</h4>
        <div className="rounded-xl bg-background p-3">
          <PulseTabs label="Sample sections" items={sectionTabs} value={section} onChange={setSection} className="sm:grid-cols-3" />
        </div>
      </div>
      <div className="grid gap-2">
        <h4 className="text-sm font-semibold">Segmented</h4>
        <PulseTabs variant="segmented" label="Sample transaction type" items={typeTabs} value={type} onChange={setType} className="justify-self-start" />
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
          <PulseDialogHeader id={titleId} description="Preview a dialog with a title and footer actions." icon={<Settings2 className="size-5" />}>Workout settings</PulseDialogHeader>
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
        <TabsPreview />
        <div className="grid content-start gap-4">
          <PreviewSection title="Cards">
            <div className="grid gap-3 rounded-xl bg-background p-3">
              <PulseCard as="article" padding="sm">
                <h4 className="font-semibold">Today’s workout</h4>
                <p className="text-sm text-muted-foreground">Surface · raised</p>
                <PulseCard size="compact" elevation="flat" padding="sm" className="mt-3 [--card-fill:var(--chip)]">
                  <p className="text-sm text-muted-foreground">Compact · flat, nested</p>
                </PulseCard>
              </PulseCard>
              <PulseCard as="article" variant="tile" padding="sm" className="[--tile-start:var(--brand-indigo)] [--tile-end:var(--brand-purple)]">
                <h4 className="font-semibold">Last workout</h4>
                <p className="text-sm opacity-85">Tile · raised</p>
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
