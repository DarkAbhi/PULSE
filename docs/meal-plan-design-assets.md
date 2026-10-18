# Meal planner artwork

Generated with the built-in image-generation tool for the Android meal planner. Assets live in `pulse-android/feature/mealplan/src/main/res/drawable-nodpi/`.

The food illustrations represent meal time categories, rather than the exact dish entered by the user. The existing light and dark themes remain supported.

## Prompts

Each subject below used this shared prompt, with `{name}` and `{subject}` substituted:

> Use case: stylized-concept. Asset type: {name} illustration for a meal planner UI. {subject} Style: cozy hand-painted digital game illustration, warm golden light, clean dark brown outlines, softly shaded, appetizing, matching a charcoal and yellow app. Isolated composition centered with generous transparent margin, slightly elevated front three-quarter view. True transparent background. No text, no UI, no watermark. Save the generated image as an asset.

- `meal_breakfast.png`: A small ceramic terracotta bowl holding a golden folded omelette with sliced eggs and fresh coriander, a few subtle leaves behind the bowl.
- `meal_lunch.png`: A small ceramic terracotta bowl brimming with orange golden biryani rice, coriander garnish and two delicate wisps of steam, a few subtle leaves behind the bowl.
- `meal_snack.png`: A small ceramic terracotta bowl filled with round golden cooked chickpeas, a few subtle olive green leaves behind the bowl.
- `meal_dinner.png`: A small ceramic terracotta bowl brimming with golden vegetable pulao rice with green peas, a few subtle muted purple leaves behind the bowl.
- `meal_empty.png`: An empty cream ceramic bowl with a few tiny crumbs and a fork with golden wooden handle resting inside, placed on a folded red and cream gingham napkin, with a small leafy green potted plant on the left and an amber water glass on the right. A few golden accent strokes above the bowl. Wide balanced still life, all objects fully visible.

## Add-meal editor

The reference's warm ivory surface and golden primary action use existing Pulse theme tokens. The editor adds a decorative salad bowl, a cutlery badge, a readable date field, wrapping time chips with sun/sunset/moon accents and animated selection colors. The existing selector shows exact time ranges and supports custom or previously assigned slots. Cancel and Save share a row when space permits and stack on narrow screens. Artwork is omitted above 130% font scale to leave room for text. Saving, validation, date picking, recovery and dirty-draft behavior use the existing flow.

`meal_plan_hero.png` was generated with the built-in image-generation tool with transparent background enabled. The PNG has true alpha and is stored beside the existing illustrations in `pulse-android/feature/mealplan/src/main/res/drawable-nodpi/`. Final prompt:

> Use case: illustration-story. Asset type: transparent decorative header illustration for an Android meal-planning form, displayed at about 140dp wide. Primary request: a lively hand-drawn cream ceramic salad bowl brimming with green lettuce, cherry tomatoes, sliced cucumber and a sunny-side-up egg, with two tiny floating leaves and three small golden sparkle diamonds around it. Style: polished cozy editorial food illustration, friendly organic shapes, dark olive-brown ink outlines, subtle watercolor shading, warm cream, golden yellow, tomato red and leafy green, matching a warm ivory UI with a golden save button. Composition: single centered bowl, front three-quarter view, wide compact arrangement, all leaves and sparkles visible, generous transparent margin. True transparent background, no background rectangle, no text, no lettering, no UI, no watermark.

`meal_sun.xml`, `meal_sunset.xml`, `meal_moon.xml` and `meal_date.xml` are native Android vector drawables in `pulse-android/feature/mealplan/src/main/res/drawable/`; the existing cutlery, edit and check icons are reused. No new dependencies are required.

Debug Compose previews in `MealEditorDesignPreviews.kt` cover phone, dark tablet and 200% text, including a custom overnight meal time. These are preview definitions, not rendered validation. UI tests were not run for this change, as requested.

