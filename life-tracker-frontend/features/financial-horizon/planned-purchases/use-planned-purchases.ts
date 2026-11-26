"use client";

import { useState } from "react";
import { addNextMonthPurchaseHorizonAction, deleteNextMonthPurchaseAction } from "@/features/financial-horizon/api/horizon-actions";
import type { HorizonMutations } from "@/features/financial-horizon/types/horizon-mutations";
import type { NextMonthPurchaseItem } from "@/features/financial-horizon/types/horizon-types";

export function usePlannedPurchases(
  initialPurchases: NextMonthPurchaseItem[],
  { setErrorMsg, startTransition }: HorizonMutations,
) {
  const [purchases, setPurchases] = useState<NextMonthPurchaseItem[]>(initialPurchases);
  const [isPlannedPurchaseDialogOpen, setIsPlannedPurchaseDialogOpen] = useState(false);
  const [purchaseToDelete, setPurchaseToDelete] = useState<NextMonthPurchaseItem | null>(null);
  const [deletingPurchaseID, setDeletingPurchaseID] = useState<number | null>(null);

  const purchasesTotal = purchases.reduce((sum, item) => sum + item.price, 0);

  const handleSavePlannedPurchase = async (data: { name: string; price: number; url: string | null }) => {
    setErrorMsg("");
    startTransition(async () => {
      const res = await addNextMonthPurchaseHorizonAction(data.name, data.price, data.url);
      if (res.ok && res.item) {
        setPurchases((prev) => [res.item, ...prev]);
        setIsPlannedPurchaseDialogOpen(false);
      } else {
        setErrorMsg(res.error ?? "Failed to add purchase item.");
      }
    });
  };

  const handleDeletePurchase = (item: NextMonthPurchaseItem) => {
    setErrorMsg("");
    setDeletingPurchaseID(item.id);
    startTransition(async () => {
      const res = await deleteNextMonthPurchaseAction(item.id);
      if (res.ok) {
        setPurchases((prev) => prev.filter((p) => p.id !== item.id));
        setPurchaseToDelete(null);
      } else {
        setErrorMsg(res.error ?? "Failed to delete purchase.");
      }
      setDeletingPurchaseID(null);
    });
  };

  return {
    purchases,
    purchasesTotal,
    isPlannedPurchaseDialogOpen,
    setIsPlannedPurchaseDialogOpen,
    purchaseToDelete,
    setPurchaseToDelete,
    deletingPurchaseID,
    handleSavePlannedPurchase,
    handleDeletePurchase,
  };
}
