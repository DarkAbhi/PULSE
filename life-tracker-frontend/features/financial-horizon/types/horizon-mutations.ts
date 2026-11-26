import type { TransitionStartFunction } from "react";
import type { HorizonUpdates } from "@/features/financial-horizon/utils/horizon-summary";
import type { SubscriptionItem, TransactionItem } from "@/features/financial-horizon/types/horizon-types";

/**
 * State shared by every Financial Horizon subdomain. The client owns one summary, one error
 * banner and one transition, so a mutation in any tab updates the same totals and pending state.
 */
export interface HorizonMutations {
  setErrorMsg: (message: string) => void;
  startTransition: TransitionStartFunction;
  recalculateSummary: (updates?: HorizonUpdates) => void;
  updateTransactions: (update: (items: TransactionItem[]) => TransactionItem[]) => void;
  updateSubscriptionsState: (update: (items: SubscriptionItem[]) => SubscriptionItem[]) => void;
}
