import {
  Activity, Bell, CarFront, CreditCard, Dumbbell, FileText, Fuel, Landmark, Moon,
  Paperclip, Receipt, ShoppingBag, Tags, Utensils, Wallet, Wind, Wrench,
} from "lucide-react";
import type { ReactNode } from "react";
import PulseCard from "@/app/components/design-system/pulse-card";
import { formatDuration } from "@/app/fitness/[id]/format-duration.mjs";
import { formatMoney } from "@/app/lib/currency.mjs";
import { calendarDayNumber, dateKey, formatCalendarDate, formatInstant } from "@/app/lib/time-zone.mjs";

const presentation = {
  fuel_fillup: { title: "Fuel fill-up", icon: Fuel },
  air_fill: { title: "Air fill logged", icon: Wind },
  maintenance: { title: "Maintenance logged", icon: Wrench },
  vehicle_added: { title: "Vehicle added", icon: CarFront },
  reminder_configured: { title: "Maintenance reminder configured", icon: Bell },
  maintenance_attachment: { title: "Maintenance attachment added", icon: Paperclip },
  meal_added: { title: "Meal added", icon: Utensils },
  meal_eaten: { title: "Meal marked as eaten", icon: Utensils },
  meal_time_added: { title: "Custom meal time added", icon: Utensils },
  workout_logged: { title: "Workout logged", icon: Dumbbell },
  workout_synced: { title: "Workout synced from iOS", icon: Dumbbell },
  exercise_added: { title: "Exercise added to workout", icon: Dumbbell },
  sleep_synced: { title: "Sleep synced", icon: Moon },
  daily_activity_synced: { title: "Daily activity synced", icon: Activity },
  transaction_logged: { title: "Transaction logged", icon: Receipt },
  statement_imported: { title: "Statement imported", icon: FileText },
  subscription_added: { title: "Subscription added", icon: CreditCard },
  subscription_payment_linked: { title: "Subscription payment linked", icon: CreditCard },
  budget_added: { title: "Budget added", icon: Wallet },
  obligation_added: { title: "Obligation added", icon: Receipt },
  planned_purchase_added: { title: "Planned purchase added", icon: ShoppingBag },
  account_added: { title: "Financial account added", icon: Landmark },
  account_archived: { title: "Financial account archived", icon: Landmark },
  account_restored: { title: "Financial account restored", icon: Landmark },
  category_added: { title: "Category added", icon: Tags },
} as const;

const scheduleLabels = {
  engine_oil: "Engine oil",
  general_service: "General service",
  tire_pressure: "Tire pressure",
};

export type RecentActivityEntry = {
  id: string;
  kind: keyof typeof presentation;
  recorded_at: string;
  details: {
    name?: string | null;
    description?: string | null;
    quantity?: number | null;
    amount?: number | null;
    odometer_km?: number | null;
    occurred_at?: string | null;
    date?: string;
    schedule_kind?: keyof typeof scheduleLabels;
    interval_km?: number;
    interval_duration?: number;
    interval_unit?: "days" | "months";
    meal_time?: string | null;
    start_time?: string;
    end_time?: string;
    exercise_count?: number;
    set_count?: number;
    duration_seconds?: number;
    distance_meters?: number | null;
    steps?: number;
    move_calories?: number;
    exercise_minutes?: number;
    transaction_type?: "debit" | "credit";
    billing_cycle?: "monthly" | "yearly";
    due_day?: number | null;
    account_type?: "bank_account" | "credit_card";
    parent_name?: string | null;
  };
};

const number = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 3 });
const rowClass =
  "relative grid grid-cols-[3rem_minmax(0,1fr)] items-center gap-4 px-6 py-4 min-[561px]:grid-cols-[3rem_minmax(0,1fr)_auto] " +
  "[&+li]:before:absolute [&+li]:before:right-6 [&+li]:before:left-22 [&+li]:before:top-0 [&+li]:before:h-px [&+li]:before:bg-divider";
const iconClass = "grid size-12 place-items-center rounded-2xl";

function dateLabel(date: string, today: string) {
  const days = calendarDayNumber(today) - calendarDayNumber(date);
  if (days === 0) return "Today";
  if (days === 1) return "Yesterday";
  const year = date.slice(0, 4) === today.slice(0, 4) ? undefined : "numeric";
  return formatCalendarDate(date, { weekday: "short", day: "numeric", month: "short", year });
}

function activityDetail(entry: RecentActivityEntry, currency: string, timeZone: string) {
  const details = entry.details;
  const parts = (...items: (string | null | undefined)[]) => items.filter(Boolean).join(" · ");
  const amount = details.amount == null ? undefined : formatMoney(details.amount, currency, "en-IN");
  const occurred = details.occurred_at ? dateKey(details.occurred_at, timeZone) : details.date;
  const recorded = dateKey(entry.recorded_at, timeZone);
  const sourceDate = occurred && occurred !== recorded
    ? formatCalendarDate(occurred, { month: "short", day: "numeric", year: occurred.slice(0, 4) === recorded.slice(0, 4) ? undefined : "numeric" })
    : undefined;
  const duration = details.duration_seconds == null ? undefined : formatDuration(details.duration_seconds, "short");
  const count = (value: number | undefined, label: string) =>
    value == null ? undefined : `${number.format(value)} ${label}${value === 1 ? "" : "s"}`;

  switch (entry.kind) {
    case "fuel_fillup":
      return parts(details.name, details.quantity == null ? undefined : `${number.format(details.quantity)} L`, amount, sourceDate);
    case "air_fill":
      return parts(details.name, details.odometer_km == null ? undefined : `${number.format(details.odometer_km)} km`, sourceDate);
    case "maintenance":
      return parts(details.name, details.description, amount, sourceDate);
    case "maintenance_attachment":
      return parts(details.name, details.description);
    case "reminder_configured": {
      const distance = details.interval_km ? `${number.format(details.interval_km)} km` : undefined;
      const interval = details.interval_duration && details.interval_unit
        ? count(details.interval_duration, details.interval_unit === "days" ? "day" : "month") : undefined;
      const schedule = [distance, interval].filter(Boolean).join(" or ");
      return parts(details.name, details.schedule_kind && scheduleLabels[details.schedule_kind], schedule && `Every ${schedule}`);
    }
    case "meal_added":
      return parts(details.name, details.meal_time, sourceDate && `Planned for ${sourceDate}`);
    case "meal_eaten":
      return parts(details.name, details.meal_time);
    case "meal_time_added":
      return parts(details.name, details.start_time && details.end_time ? `${details.start_time}–${details.end_time}` : undefined);
    case "workout_logged":
      return parts(count(details.exercise_count, "exercise"), count(details.set_count, "set"), sourceDate);
    case "workout_synced":
      return parts(details.name, duration,
        details.distance_meters ? `${number.format(details.distance_meters / 1000)} km` : undefined, sourceDate);
    case "exercise_added":
      return parts(details.name, sourceDate);
    case "sleep_synced":
      return parts(duration && `${duration} asleep`, sourceDate);
    case "daily_activity_synced":
      return parts(count(details.steps, "step"),
        details.move_calories == null ? undefined : `${number.format(details.move_calories)} kcal moved`,
        details.exercise_minutes == null ? undefined : `${details.exercise_minutes} min exercise`, sourceDate);
    case "transaction_logged":
      return parts(details.name, amount && `${amount}${details.transaction_type ? ` ${details.transaction_type}` : ""}`, sourceDate);
    case "subscription_added":
      return parts(details.name, amount && `${amount}/${details.billing_cycle === "yearly" ? "year" : "month"}`);
    case "budget_added":
      return parts(details.name, amount && `${amount}/month`);
    case "obligation_added":
      return parts(details.name, amount, details.due_day ? `Due on day ${details.due_day}` : undefined);
    case "planned_purchase_added":
      return parts(details.name, amount, details.date && formatCalendarDate(details.date, { month: "long", year: "numeric" }));
    case "account_added":
      return parts(details.name, details.account_type === "credit_card" ? "Credit card" : "Bank account");
    case "category_added":
      return parts(details.parent_name, details.name);
    case "subscription_payment_linked":
      return parts(details.name, amount);
    default:
      return details.name;
  }
}

function ActivitySection({ children, loading }: { children: ReactNode; loading?: boolean }) {
  return (
    <section aria-labelledby="recent-activity-heading" aria-busy={loading} className="grid gap-5">
      <header className="flex min-h-11 items-center">
        <h2 id="recent-activity-heading" className="text-[clamp(1.4rem,2.4vw,1.8rem)] leading-[1.15] font-extrabold tracking-[-.035em]">
          Recent activity
        </h2>
      </header>
      <PulseCard className="overflow-hidden shadow-(--dashboard-activity-shadow)">
        {children}
      </PulseCard>
    </section>
  );
}

export function RecentActivitySkeleton() {
  return (
    <ActivitySection loading>
      <ul aria-hidden="true">
        {[0, 1, 2].map(row => (
          <li key={row} className={rowClass}>
            <div className={`${iconClass} bg-(--dashboard-activity-chip)`} />
            <div className="grid gap-2">
              <div className="h-4 w-32 rounded bg-muted" />
              <div className="h-3 w-2/5 rounded bg-muted" />
            </div>
            <div className="col-start-2 h-3 w-12 rounded bg-muted min-[561px]:col-start-3" />
          </li>
        ))}
      </ul>
    </ActivitySection>
  );
}

export function RecentActivity({ entries, timeZone, currency }: {
  entries: RecentActivityEntry[] | null;
  timeZone: string;
  currency: string;
}) {
  const today = dateKey(Date.now(), timeZone);

  return (
    <ActivitySection>
      {entries?.length ? (
        <ul>
          {entries.map(entry => {
            const event = presentation[entry.kind];
            if (!event) return null;
            const detail = activityDetail(entry, currency, timeZone);
            const title = entry.kind === "category_added" && entry.details.parent_name ? "Subcategory added" : event.title;
            return (
              <li key={entry.id} className={rowClass}>
                <span className={`${iconClass} bg-[color-mix(in_srgb,var(--brand-blue)_14%,transparent)] text-(--brand-blue)`} aria-hidden="true">
                  <event.icon className="size-5" />
                </span>
                <div className="min-w-0 wrap-anywhere">
                  <p className="text-[1.05rem] font-extrabold tracking-[-.01em]">{title}</p>
                  {detail && <p className="mt-px text-[.92rem] text-muted-foreground">{detail}</p>}
                </div>
                <time className="col-start-2 self-start text-[.9rem] font-semibold whitespace-nowrap text-muted-foreground min-[561px]:col-start-3 min-[561px]:mt-1"
                  dateTime={entry.recorded_at} title={formatInstant(entry.recorded_at, timeZone)}>
                  {dateLabel(dateKey(entry.recorded_at, timeZone), today)}
                </time>
              </li>
            );
          })}
        </ul>
      ) : (
        <p className="p-6 text-muted-foreground">{entries ? "No recent activity yet." : "Recent activity is unavailable."}</p>
      )}
    </ActivitySection>
  );
}
