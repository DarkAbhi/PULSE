export default function FuelTypeDot({ fuel }: { fuel: string }) {
  return (
    <span
      aria-hidden="true"
      data-fuel={fuel}
      className="size-[0.55rem] flex-none rounded-full bg-[#9086ab] data-[fuel=petrol]:bg-[#ff8c42] data-[fuel=diesel]:bg-[#526bff] data-[fuel=cng]:bg-[#38b38e] data-[fuel=lpg]:bg-[#aa6ee5] data-[fuel=electric]:bg-[#36b4db]"
    />
  );
}
