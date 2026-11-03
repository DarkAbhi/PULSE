"use client";

import { Fuel, Plus } from "lucide-react";
import { SubmitEvent } from "react";
import Button from "./design-system/button";
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

const controlClass = "mt-1.5 block min-h-11 w-full min-w-0 rounded-lg border bg-card px-3 py-2 text-base text-foreground focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring";
const labelClass = "min-w-0 text-sm font-medium text-foreground";

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
    <form className="mt-5 space-y-5" onSubmit={onSubmit}>
      <div className="grid gap-3 sm:grid-cols-2">
        <label className={labelClass}>
          Odometer (km)
          <input className={`${controlClass} border-input tabular-nums`} min="0" onChange={(event) => onOdometerChange(event.target.value)} required step="0.1" type="number" value={odometer} />
        </label>
        <label className={labelClass}>
          Date &amp; time
          <input className={`${controlClass} border-input`} onChange={(event) => onFilledAtChange(event.target.value)} required type="datetime-local" value={filledAt} />
        </label>
      </div>

      <div className="space-y-3">
        <p className="text-sm text-muted-foreground">
          Enter any two amounts per tank. Pulse calculates the third when you save.
        </p>
        {items.map((item, index) => (
          <fieldset className="min-w-0 rounded-xl border border-border bg-background p-3 sm:p-4" key={index}>
            <legend className="sr-only">{items.length > 1 ? `Tank ${index + 1}` : "Fuel details"}</legend>
            <div aria-hidden="true" className="mb-3 flex items-center gap-2 text-sm font-semibold text-foreground">
              <Fuel className="size-4 text-action" />
              {items.length > 1 ? `Tank ${index + 1}` : "Fuel details"}
            </div>
            <div className="grid grid-cols-2 gap-3">
              <label className={labelClass}>
                Fuel type
                <select className={`${controlClass} border-input`} onChange={(event) => updateItem(index, "fuelType", event.target.value)} value={item.fuelType}>
                  <option value="petrol">Petrol</option>
                  <option value="diesel">Diesel</option>
                  <option value="lpg">LPG</option>
                  <option value="cng">CNG</option>
                  <option value="electric">Electric</option>
                </select>
              </label>
              <label className={labelClass}>
                Fill type
                <select className={`${controlClass} border-input`} onChange={(event) => updateItem(index, "fillType", event.target.value)} value={item.fillType}>
                  <option value="full">Full tank</option>
                  <option value="partial">Partial fill-up</option>
                  <option value="missed">Missed fill-up</option>
                </select>
              </label>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-3 border-t border-dashed border-input pt-3 sm:grid-cols-3">
              {([["quantity", "Quantity (L)"], ["unitPrice", "Price per litre"]] as const).map(([field, label]) => (
                <label className={labelClass} key={field}>
                  {label}
                  <input className={`${controlClass} border-input tabular-nums`} min="0" onChange={(event) => updateItem(index, field, event.target.value)} step="0.01" type="number" value={item[field]} />
                </label>
              ))}
              <label className={`${labelClass} col-span-2 sm:col-span-1`}>
                Total cost
                <input className={`${controlClass} border-action font-semibold tabular-nums`} min="0" onChange={(event) => updateItem(index, "totalCost", event.target.value)} step="0.01" type="number" value={item.totalCost} />
              </label>
            </div>
          </fieldset>
        ))}
        {items.length < 2 && (
          <Button variant="tertiary" size="md" icon={<Plus aria-hidden="true" className="size-4" />} onClick={() => onItemsChange([...items, newFuelItem()])} type="button">
            Add second fuel tank
          </Button>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <label className={labelClass}>
          Station / vendor
          <input className={`${controlClass} border-input`} onChange={(event) => onStationNameChange(event.target.value)} placeholder="Optional" value={stationName} />
        </label>
        <label className={labelClass}>
          Notes
          <input className={`${controlClass} border-input`} onChange={(event) => onNotesChange(event.target.value)} placeholder="Optional" value={notes} />
        </label>
      </div>
      {error && <p className="rounded-lg bg-destructive-soft px-3 py-2 text-sm text-destructive" role="alert">{error}</p>}
      <DialogActions>
        <DialogAction variant="secondary" disabled={isSaving} onClick={onCancel} type="button">Cancel</DialogAction>
        <DialogAction disabled={isSaving} type="submit">{isSaving ? savingLabel : submitLabel}</DialogAction>
      </DialogActions>
    </form>
  );
}
