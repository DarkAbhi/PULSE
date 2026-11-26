export default function FuelTypeDot({ fuel }: { fuel: string }) {
  return (
    <span
      aria-hidden="true"
      data-fuel={fuel}
      className="size-2 flex-none rounded-full bg-garage-fuel-other data-[fuel=petrol]:bg-garage-fuel-petrol data-[fuel=diesel]:bg-garage-fuel-diesel data-[fuel=cng]:bg-garage-fuel-cng data-[fuel=lpg]:bg-garage-fuel-lpg data-[fuel=electric]:bg-garage-fuel-electric"
    />
  );
}
