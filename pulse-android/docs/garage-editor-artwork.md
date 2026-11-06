# Add-vehicle artwork

Generated with the built-in ImageGen tool for the tablet add-vehicle introduction.
The images are decorative; they do not represent the selected vehicle type or change the saved form.

Final assets are transparent 768 × 768 WebP files in `feature/garage/src/main/res/drawable-nodpi/`:

- `garage_scooter.webp`
- `garage_motorcycle.webp`
- `garage_car.webp`

The artwork cycles every eight seconds, including a 2.4-second linear crossfade.
The timer runs only while the screen is resumed. Disabling system animations keeps the artwork still.
Phone and large-text layouts keep the compact, text-only introduction.

## Generation prompts

Each image was generated separately with this shared prompt, replacing `VEHICLE` with its subject below:

> Use case: product-mockup. Create a premium realistic 3D product illustration for a warm cream and yellow Android garage form. One complete unbranded VEHICLE, three-quarter front view facing right, wheels on the same ground line, centered, whole vehicle visible with generous transparent margin, square canvas. Warm soft studio lighting, charcoal and muted ivory body with subtle golden yellow accents, detailed realistic tires and materials, subtle contact shadow only. Transparent background. No people, typography, logos, scenery, pedestal or frame. Elegant catalog-quality rendering, consistent camera angle and light.

Subjects:

- Scooter: modern step-through scooter with compact wheels and a comfortable seat.
- Bike: adventure motorcycle with a round headlight, windscreen, sculpted fuel tank and rugged wheels.
- Car: compact modern hatchback car with four wheels.

The generated PNGs were resized and encoded with `cwebp`, retaining transparency.
