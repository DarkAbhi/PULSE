import type { ReactNode } from "react";
import { Check, Plus, X } from "lucide-react";
import PulseButton from "@/components/ui/pulse-button";
import PulseCard from "@/components/ui/pulse-card";
import PulseIconButton from "@/components/ui/pulse-icon-button";
import type { MealPlan } from "@/features/meal-plan/api/meal-plan-actions";
import { mealSlotColors, type MealSlot } from "@/features/meal-plan/components/meal-visuals";
import { mealPlanStyles as styles } from "@/features/meal-plan/styles/meal-plan-classes";

export function MealSlotCard({ kind, icon, name, timeRange, usualTime, children, onAdd, addDisabled }: {
  kind: MealSlot;
  icon: ReactNode;
  name: string;
  timeRange?: string;
  usualTime?: string;
  children: ReactNode;
  onAdd?: () => void;
  addDisabled?: boolean;
}) {
  return (
    <PulseCard as="article" className={styles.slotCard}>
      <div className="flex items-start gap-3">
        <span className={`${styles.slotIcon} ${mealSlotColors[kind]} ${kind === "breakfast" || kind === "lunch" ? "text-(--meal-slot-ink)" : "text-white"}`} aria-hidden="true">{icon}</span>
        <div className="min-w-0 self-center">
          <h3 className="text-lg leading-tight font-extrabold tracking-tight wrap-anywhere">{name}</h3>
          {timeRange && <p className={`${styles.muted} mt-1 text-sm font-semibold`}>{timeRange}</p>}
          {usualTime && <p className={`${styles.muted} mt-1 text-xs`}>Your usual time: {usualTime}</p>}
        </div>
      </div>
      {children}
      {onAdd && (
        <PulseButton variant="ghost" size="xl" labelStyle="strong" className={styles.slotAdd} type="button"
          onClick={onAdd} disabled={addDisabled} aria-label={`Add meal to ${name}`} icon={<Plus className="size-5" aria-hidden="true" />}
        >
          Add meal
        </PulseButton>
      )}
    </PulseCard>
  );
}

export function MealList({ meals, disabled, onToggleConsumed, onDelete }: {
  meals: MealPlan[];
  disabled: boolean;
  onToggleConsumed: (meal: MealPlan) => void;
  onDelete: (meal: MealPlan) => void;
}) {
  return (
    <ul className="flex flex-col gap-2">
      {meals.map((meal) => (
        <li key={meal.id} className={styles.mealRow}>
          <button
            type="button"
            role="checkbox"
            aria-checked={meal.is_consumed}
            aria-label={`Eaten: ${meal.name}`}
            onClick={() => onToggleConsumed(meal)}
            disabled={disabled}
            className={styles.mealToggle}
          >
            <span className={styles.check} aria-hidden="true">
              {meal.is_consumed && <Check className="size-5" strokeWidth={3} />}
            </span>
            <span className={`min-w-0 wrap-anywhere text-sm font-semibold ${meal.is_consumed ? `${styles.muted} line-through` : ""}`}>
              {meal.name}
            </span>
          </button>
          <PulseIconButton
            type="button"
            variant="danger"
            size="lg"
            className="shrink-0"
            onClick={() => onDelete(meal)}
            aria-label={`Delete ${meal.name}`}
            disabled={disabled}
          >
            <X className="size-5" aria-hidden="true" />
          </PulseIconButton>
        </li>
      ))}
    </ul>
  );
}
