import { Utensils } from "lucide-react";
import QuickActionCard from "@/features/dashboard/components/quick-actions/quick-action-card";
import MealPlanWeekStrip from "@/features/dashboard/components/quick-actions/meal-plan-week-strip";

export default function MealPlanCard() {
  return (
    <QuickActionCard
      tone="meal"
      href="/meal-plan"
      title="Meal Plan"
      description="Plan your week and track meals eaten"
      icon={<Utensils strokeWidth={1.8} />}
      footer={<MealPlanWeekStrip />}
    />
  );
}
