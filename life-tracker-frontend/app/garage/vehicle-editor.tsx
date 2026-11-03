"use client";

import { useEffect, useId, useState, useTransition } from "react";
import { CarFront, ListFilter, PencilLine, Plus } from "lucide-react";
import Button from "@/app/components/design-system/button";
import Dialog, {
  DialogAction,
  DialogActions,
} from "@/app/components/design-system/dialog";
import { catalogOptionsAction, saveVehicleAction } from "./actions";

export type VehicleIdentity = {
  id?: number | string;
  name: string;
  kind: string;
  catalog_id: string | null;
  make_name: string;
  model_name: string;
  fuel_types: string[];
};
type Option = { id: string; name: string };
const kinds = ["car", "motorcycle", "moped", "van", "truck", "bus"];
const fuels = ["petrol", "diesel", "lpg", "cng", "electric"];
const field =
  "w-full rounded-lg border border-border bg-background px-4 py-3 text-foreground outline-none focus:border-primary focus:ring-4 focus:ring-primary/15";
const titleCase = (value: string) =>
  value === "lpg" || value === "cng"
    ? value.toUpperCase()
    : value.charAt(0).toUpperCase() + value.slice(1);

function CatalogSelect({
  label,
  kind,
  make,
  value,
  initialName,
  onChange,
}: {
  label: "Make" | "Model";
  kind: string;
  make: string;
  value: string;
  initialName: string;
  onChange: (option: Option) => void;
}) {
  const id = useId();
  const [search, setSearch] = useState("");
  const [options, setOptions] = useState<Option[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    setOptions([]);
    const timer = setTimeout(async () => {
      const result = await catalogOptionsAction(
        label === "Make" ? "makes" : "models",
        kind,
        make,
        search,
      );
      if (!active) return;
      setLoading(false);
      if (result.ok) setOptions(result.options);
      else setError(result.error);
    }, 200);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [label, kind, make, search, retry]);
  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium" htmlFor={`${id}-search`}>
        Search {label.toLowerCase()}s
      </label>
      <input
        id={`${id}-search`}
        className={field}
        type="search"
        maxLength={100}
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder={`Search ${label.toLowerCase()}s…`}
      />
      <label className="block text-sm font-medium" htmlFor={id}>
        {label}
      </label>
      <select
        id={id}
        className={field}
        required
        value={value}
        onChange={(e) => {
          const option = options.find((o) => o.id === e.target.value);
          if (option) onChange(option);
        }}
      >
        <option value="">Select {label.toLowerCase()}</option>
        {value && !options.some((o) => o.id === value) && (
          <option value={value}>{initialName}</option>
        )}
        {options.map((option) => (
          <option key={option.id} value={option.id}>
            {option.name}
          </option>
        ))}
      </select>
      <p className="text-sm text-muted-foreground" role="status">
        {loading
          ? "Loading…"
          : !error && options.length === 0
            ? "No matches. Try another search or enter your vehicle manually."
            : !error && options.length === 100
              ? "Showing 100 matches. Search to narrow the list."
              : ""}
      </p>
      {error && (
        <div role="alert" className="text-sm text-destructive">
          {error}{" "}
          <button
            type="button"
            className="underline"
            onClick={() => setRetry((n) => n + 1)}
          >
            Retry
          </button>
        </div>
      )}
    </div>
  );
}

export default function VehicleEditor({
  vehicle,
}: {
  vehicle?: VehicleIdentity;
}) {
  const uid = useId();
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState(
    vehicle && !vehicle.catalog_id ? "manual" : "catalog",
  );
  const [name, setName] = useState(vehicle?.name ?? "");
  const [kind, setKind] = useState(
    vehicle?.kind === "unknown" ? "" : (vehicle?.kind ?? ""),
  );
  const [make, setMake] = useState(vehicle?.catalog_id?.split("/")[1] ?? "");
  const [makeName, setMakeName] = useState(vehicle?.make_name ?? "");
  const [modelName, setModelName] = useState(vehicle?.model_name ?? "");
  const [catalog, setCatalog] = useState(vehicle?.catalog_id ?? "");
  const [fuel, setFuel] = useState(vehicle?.fuel_types[0] ?? "");
  const [secondFuel, setSecondFuel] = useState(vehicle?.fuel_types[1] ?? "");
  const [dual, setDual] = useState((vehicle?.fuel_types.length ?? 0) > 1);
  const [error, setError] = useState("");
  const [pending, startTransition] = useTransition();
  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    startTransition(async () => {
      const result = await saveVehicleAction(vehicle?.id, {
        name: name.trim(),
        kind,
        catalog_id: mode === "catalog" ? catalog : "",
        make_name: makeName,
        model_name: modelName,
        fuel_types: dual ? [fuel, secondFuel] : [fuel],
      });
      if (!result.ok) {
        setError(result.error ?? "We couldn't save this vehicle.");
        return;
      }
      setOpen(false);
      if (!vehicle) {
        setName("");
        setKind("");
        setMake("");
        setMakeName("");
        setModelName("");
        setCatalog("");
        setFuel("");
        setSecondFuel("");
        setDual(false);
      }
    });
  }
  return (
    <>
      <Button
        id={vehicle ? undefined : "add-vehicle-button"}
        variant={vehicle ? "secondary" : "primary"}
        icon={vehicle ? undefined : <Plus className="size-4" aria-hidden="true" />}
        type="button"
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={() => {
          setError("");
          setOpen(true);
        }}
      >
        {vehicle ? "Edit vehicle" : "Add vehicle"}
      </Button>
      {open && (
        <Dialog size="lg" labelledBy={`${uid}-title`}>
          <header className="flex items-start gap-3 rounded-xl bg-secondary p-4 sm:gap-4 sm:p-5">
            <span className="grid size-11 shrink-0 place-items-center rounded-lg bg-accent text-accent-foreground">
              <CarFront className="size-6" strokeWidth={1.6} aria-hidden="true" />
            </span>
            <div>
              <h2 id={`${uid}-title`} className="text-2xl font-bold tracking-tight">
                {vehicle ? "Edit vehicle" : "Add a vehicle"}
              </h2>
              <p className="mt-1 text-sm text-muted-foreground">
                Choose your vehicle from the catalog or enter its details manually.
              </p>
            </div>
          </header>
          <form className="mt-6 space-y-6" onSubmit={submit}>
            <fieldset className="grid min-w-0 gap-2 sm:grid-cols-2">
              <legend className="mb-2 text-sm font-semibold">Entry method</legend>
              {["catalog", "manual"].map((option) => (
                <label key={option} className="flex min-h-11 cursor-pointer items-center gap-2.5 rounded-lg border border-border px-3 py-2.5 text-sm font-medium has-checked:border-action has-checked:bg-accent has-checked:text-accent-foreground">
                  <input
                    type="radio"
                    className="shrink-0 accent-action focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
                    name={`${uid}-mode`}
                    value={option}
                    checked={mode === option}
                    onChange={() => {
                      setMode(option);
                      if (option === "catalog") {
                        setMake("");
                        setCatalog("");
                        setMakeName("");
                        setModelName("");
                      }
                    }}
                  />
                  {option === "catalog"
                    ? <ListFilter className="size-4 shrink-0" aria-hidden="true" />
                    : <PencilLine className="size-4 shrink-0" aria-hidden="true" />}
                  {option === "catalog" ? "Choose from catalog" : "Enter manually"}
                </label>
              ))}
            </fieldset>
            <fieldset className="min-w-0 space-y-4">
              <legend className="mb-2 text-base font-semibold">Vehicle details</legend>
              <div className="space-y-2">
                <label
                  className="block text-sm font-medium"
                  htmlFor={`${uid}-kind`}
                >
                  Vehicle type
                </label>
                <select
                  id={`${uid}-kind`}
                  required
                  className={field}
                  value={kind}
                  onChange={(e) => {
                    setKind(e.target.value);
                    setMake("");
                    setCatalog("");
                    setMakeName("");
                    setModelName("");
                  }}
                >
                  <option value="">Select vehicle type</option>
                  {kinds.map((k) => (
                    <option key={k} value={k}>
                      {titleCase(k)}
                    </option>
                  ))}
                </select>
              </div>
              {mode === "catalog" && kind && (
                <CatalogSelect
                  key={kind}
                  label="Make"
                  kind={kind}
                  make=""
                  value={make}
                  initialName={makeName}
                  onChange={(option) => {
                    setMake(option.id);
                    setMakeName(option.name);
                    setCatalog("");
                    setModelName("");
                  }}
                />
              )}
              {mode === "catalog" && kind && make && (
                <CatalogSelect
                  key={`${kind}/${make}`}
                  label="Model"
                  kind={kind}
                  make={make}
                  value={catalog}
                  initialName={modelName}
                  onChange={(option) => {
                    setCatalog(option.id);
                    setModelName(option.name);
                    if (!name || name === `${makeName} ${modelName}`.trim())
                      setName(`${makeName} ${option.name}`.trim());
                  }}
                />
              )}
              {mode === "manual" && (
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <label
                      className="block text-sm font-medium"
                      htmlFor={`${uid}-make`}
                    >
                      Make (optional)
                    </label>
                    <input
                      id={`${uid}-make`}
                      className={field}
                      maxLength={200}
                      value={makeName}
                      onChange={(e) => setMakeName(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <label
                      className="block text-sm font-medium"
                      htmlFor={`${uid}-model`}
                    >
                      Model (optional)
                    </label>
                    <input
                      id={`${uid}-model`}
                      className={field}
                      maxLength={200}
                      value={modelName}
                      onChange={(e) => setModelName(e.target.value)}
                    />
                  </div>
                </div>
              )}
              <div className="space-y-2">
                <label
                  className="block text-sm font-medium"
                  htmlFor={`${uid}-name`}
                >
                  Vehicle name
                </label>
                <input
                  id={`${uid}-name`}
                  className={field}
                  required
                  maxLength={48}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Model name or a personal nickname"
                />
              </div>
            </fieldset>
            <fieldset className="min-w-0 space-y-3">
              <legend className="mb-2 w-full border-t border-divider pt-4 text-base font-semibold">Fuel setup</legend>
              <label
                className="block text-sm font-medium"
                htmlFor={`${uid}-fuel`}
              >
                Fuel type
              </label>
              <select
                id={`${uid}-fuel`}
                className={field}
                required
                value={fuel}
                onChange={(e) => {
                  setFuel(e.target.value);
                  if (e.target.value === secondFuel) setSecondFuel("");
                }}
              >
                <option value="">Select fuel type</option>
                {fuels.map((f) => (
                  <option key={f} value={f}>
                    {titleCase(f)}
                  </option>
                ))}
              </select>
              <label className="flex min-h-11 cursor-pointer items-center gap-3 rounded-lg bg-secondary px-3 text-sm">
                <input
                  type="checkbox"
                  className="size-4 accent-action focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring"
                  checked={dual}
                  onChange={(e) => setDual(e.target.checked)}
                />
                Dual tank (two fuel types)
              </label>
              {dual && (
                <>
                  <label
                    className="block text-sm font-medium"
                    htmlFor={`${uid}-second`}
                  >
                    Second fuel type
                  </label>
                  <select
                    id={`${uid}-second`}
                    className={field}
                    required
                    value={secondFuel}
                    onChange={(e) => setSecondFuel(e.target.value)}
                  >
                    <option value="">Select second fuel type</option>
                    {fuels
                      .filter((f) => f !== fuel)
                      .map((f) => (
                        <option key={f} value={f}>
                          {titleCase(f)}
                        </option>
                      ))}
                  </select>
                </>
              )}
            </fieldset>
            {error && (
              <p role="alert" className="rounded-lg bg-destructive-soft p-3 text-sm text-destructive">
                {error}
              </p>
            )}
            <p className="text-xs text-muted-foreground">
              Vehicle data by{" "}
              <a
                className="text-action underline"
                href="https://vehiclesdb.com"
              >
                VehiclesDB
              </a>{" "}
              ·{" "}
              <a className="text-action underline" href="/vehicle-data">
                Sources and credits
              </a>
            </p>
            <DialogActions className="grid-cols-2 border-t border-divider pt-4">
              <DialogAction
                type="button"
                variant="secondary"
                disabled={pending}
                onClick={() => setOpen(false)}
              >
                Cancel
              </DialogAction>
              <DialogAction
                id="add-vehicle-submit"
                type="submit"
                disabled={pending || (mode === "catalog" && !catalog)}
              >
                {pending ? "Saving…" : vehicle ? "Save changes" : "Add vehicle"}
              </DialogAction>
            </DialogActions>
          </form>
        </Dialog>
      )}
    </>
  );
}
