"use client";

import { SubmitEvent } from "react";
import { DialogAction, DialogActions } from "./design-system/dialog";

export type FuelFormItem = {
  fuelType: string;
  fillType: string;
  quantity: string;
  unitPrice: string;
  totalCost: string;
};

type FuelFormProps = {
  items: FuelFormItem[];
  odometer: string;
  filledAt: string;
  stationName: string;
  notes: string;
  isSaving: boolean;
  error: string;
  submitLabel: string;
  savingLabel: string;
  onSubmit: (event: SubmitEvent<HTMLFormElement>) => void;
  onCancel: () => void;
  onOdometerChange: (value: string) => void;
  onFilledAtChange: (value: string) => void;
  onStationNameChange: (value: string) => void;
  onNotesChange: (value: string) => void;
  onItemsChange: (items: FuelFormItem[]) => void;
};

const newFuelItem = (): FuelFormItem => ({
  fuelType: "petrol",
  fillType: "full",
  quantity: "",
  unitPrice: "",
  totalCost: "",
});

export function FuelForm({
  items,
  odometer,
  filledAt,
  stationName,
  notes,
  isSaving,
  error,
  submitLabel,
  savingLabel,
  onSubmit,
  onCancel,
  onOdometerChange,
  onFilledAtChange,
  onStationNameChange,
  onNotesChange,
  onItemsChange,
}: FuelFormProps) {
  function updateItem(index: number, field: keyof FuelFormItem, value: string) {
    onItemsChange(items.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item));
  }

  return (
    <form className="mt-6 space-y-5" onSubmit={onSubmit}>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="text-sm font-medium text-muted-foreground">Odometer (km)<input className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2" min="0" onChange={(event) => onOdometerChange(event.target.value)} required step="0.1" type="number" value={odometer} /></label>
        <label className="text-sm font-medium text-muted-foreground">Date &amp; time<input className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2" onChange={(event) => onFilledAtChange(event.target.value)} required type="datetime-local" value={filledAt} /></label>
        <label className="text-sm font-medium text-muted-foreground">Station / vendor<input className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2" onChange={(event) => onStationNameChange(event.target.value)} placeholder="Optional" value={stationName} /></label>
        <label className="text-sm font-medium text-muted-foreground">Notes<input className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2" onChange={(event) => onNotesChange(event.target.value)} placeholder="Optional" value={notes} /></label>
      </div>
      {items.map((item, index) => (
        <fieldset className="rounded-xl border border-border p-4" key={index}>
          <legend className="px-1 text-sm font-semibold text-foreground">{items.length > 1 ? `Tank ${index + 1}` : "Fuel details"}</legend>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="text-sm">Fuel type<select className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2" onChange={(event) => updateItem(index, "fuelType", event.target.value)} value={item.fuelType}><option value="petrol">Petrol</option><option value="diesel">Diesel</option><option value="lpg">LPG</option><option value="cng">CNG</option><option value="electric">Electric</option></select></label>
            <label className="text-sm">Fill type<select className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2" onChange={(event) => updateItem(index, "fillType", event.target.value)} value={item.fillType}><option value="full">Full tank</option><option value="partial">Partial fill-up</option><option value="missed">Missed fill-up</option></select></label>
            {([['quantity', 'Quantity (L)'], ['unitPrice', 'Price per litre'], ['totalCost', 'Total cost']] as const).map(([field, label]) => <label className="text-sm" key={field}>{label}<input className="mt-1 w-full rounded-lg border border-border bg-background text-foreground px-3 py-2" min="0" onChange={(event) => updateItem(index, field, event.target.value)} step="0.01" type="number" value={item[field]} /></label>)}
          </div>
        </fieldset>
      ))}
      {items.length < 2 && <button className="cursor-pointer text-sm font-semibold text-primary" onClick={() => onItemsChange([...items, newFuelItem()])} type="button">+ Add second fuel tank</button>}
      {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
      <DialogActions><DialogAction variant="secondary" disabled={isSaving} onClick={onCancel} type="button">Cancel</DialogAction><DialogAction disabled={isSaving} type="submit">{isSaving ? savingLabel : submitLabel}</DialogAction></DialogActions>
    </form>
  );
}
