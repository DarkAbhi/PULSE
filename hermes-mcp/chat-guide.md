# What you can ask Hermes to do in Pulse

Send these messages in your Hermes chat. Replace the dates, vehicle names, and numbers with yours. Hermes should show the proposed changes before saving. Reply “Yes, save it” after checking the details, or send a correction and confirm the revised summary.

## Workouts

| What you can do | Example message | Hermes tool |
| --- | --- | --- |
| Check whether you worked out today | “Did I work out today?” | `get_workout_today` |
| Read session details, type, focus, and sets | “What did I do in my workout on 2 October 2026? Show me the type, focus, and each set.” | `get_workout_sessions` |
| Look up an exercise or find the right variant | “Can you look up machine rear delt fly in the exercise catalogue?” | `search_exercises` |
| Add exercises and sets to an existing workout | “Add to today's workout: barbell bench press, 40 kg for 10 reps, then 45 kg for 8 and 6 reps. I also did 3 sets of 10 bodyweight push-ups.” | `add_exercises_to_workout` |
| Save type and focus with your sets | “For my 2 October 2026 workout, set the type to single muscle and the focus to shoulders, and add these sets.” Use the full example below. | `add_exercises_to_workout` |
| Change type or focus on an existing workout | “My 2 October 2026 workout was a pull workout with a back and biceps focus.” Or “Change just the focus of that workout to rear delts.” | `add_exercises_to_workout` |
| Clear the focus or classification | “Clear the focus on my 2 October workout. Keep the workout type.” Or “Clear the workout type and focus from that session.” | `add_exercises_to_workout` |
| Choose between visits on the same date | “Use the afternoon visit on 2 October 2026. Add 3 sets of seated dumbbell shoulder press, each 10 reps at 10 kg per dumbbell.” | `get_workout_sessions`, `add_exercises_to_workout` |
| Remember a catalogue match when saving | “Link ‘weight plate front raises’ to Front Plate Raise and remember that wording for next time. Save it for today's workout, 3 sets of 10 reps at 10 kg.” | `search_exercises`, `add_exercises_to_workout` |

### Copy-ready shoulder workout

> For my 2 October 2026 workout, set the workout type to single muscle and the focus to shoulders. Add these exercises:
>
> - Dumbbell shoulder press: 7.5 kg × 12, 10 kg × 12, 12.5 kg × 12. These weights are per dumbbell.
> - Weight plate front raises: 10 kg × 10 for all 3 sets.
> - Dumbbell lateral raises: 5 kg × 10 for all 3 sets, per dumbbell.
> - Rear delt fly: 25 kg × 10, 32 kg × 10, 32 kg × 10.

Supported workout types are **Single muscle, Double muscle, Push, Pull, Legs, Upper body, Lower body, Full body, Cardio, Mobility, and Other**. The focus is optional free text up to 120 characters. A visit must already exist in Pulse. Use the app to log a visit, change the weekly workout schedule or set a rest day, or edit or delete saved exercises. Hermes appends exercises to the selected visit.

Dated session lookups and saves use India time. Without a date, Hermes uses today in India time for those actions. The quick “Did I work out today?” status check uses your Pulse profile time zone. If the year, visit, exercise variant, or set details are unclear, Hermes should ask before saving.

### How Hermes saves a workout

1. Look up the date using `get_workout_sessions`. If there are multiple visits, show the times and ask which visit to use. Pass the confirmed date as `YYYY-MM-DD` and the selected `visit_id` when saving.
2. Search the exercise catalogue before saving exercises. Use the IDs returned by the search. Unique exact or remembered matches can be used directly. For other suggestions, ask the user to choose the equipment or variant and search again if needed. For example, a machine rear delt fly may be listed as “reverse machine flyes.”
3. Show the chosen visit, classification changes, catalogue matches, and every set in order. Preserve the original exercise names and the reported weights in kilograms, including per-dumbbell weights. Use `null` for an explicitly unweighted set or an unmatched catalogue ID. Set `remember_alias=true` only when the user requests it.
4. Save with `confirmed=true` after approval. For exercises alone, omit the classification fields. When changing type or focus, send the full intended classification, carrying forward any saved field the user wants to keep. If a save times out, read the session to check whether it committed before retrying.

## Garage

| What you can do | Example message | Hermes tool |
| --- | --- | --- |
| List vehicles | “What vehicles are in my Pulse garage?” | `get_vehicles` |
| Read service history | “Show the maintenance history for my Himalayan 411, including dates and odometer readings.” | `get_vehicle_maintenance` |
| Find the last time a part was replaced | “When was my Himalayan's clutch cable last replaced? Include the odometer and invoice reference if available.” | `get_vehicle_maintenance` |
| Filter by work performed | “Show the times the Himalayan's chain was cleaned or lubricated.” | `get_vehicle_maintenance` |
| See what's due and inspect saved reminder intervals | “What's coming up for my Himalayan? Show my oil, general service, and tyre-pressure settings.” | `get_vehicle_maintenance` |
| Record completed service with multiple actions | “Record the Himalayan's full service on 10 November 2026 at 24,800 km. I paid ₹3,200 at Royal Enfield. They replaced the engine oil and oil filter, adjusted the clutch cable, and lubricated the chain.” | `record_vehicle_maintenance` |
| Record a top-up, inspection, repair, or free DIY work | “On 12 November 2026 at 24,940 km I topped up the Himalayan's engine oil myself. Cost ₹0.” | `record_vehicle_maintenance` |
| Record another garage expense that has a maintenance category | “Log the Himalayan's insurance renewal on 10 November 2026 for ₹4,500.” | `record_vehicle_maintenance` |
| Set a personal reminder interval | “For my Himalayan, set the personal oil-change interval to 4,000 km or 6 months. Make Personal active and keep my Manual settings.” | `configure_vehicle_reminder` |
| Set or switch to a manual interval | “For my Himalayan's general service, set Manual to 5,000 km or 6 months, reference ‘owner's manual, page 42,’ and make it active.” | `configure_vehicle_reminder` |
| Set a time-only tyre-pressure reminder | “Set the Himalayan's personal tyre-pressure check to every 1 month with the distance threshold disabled. Make Personal active.” | `configure_vehicle_reminder` |

Pick the vehicle by its saved name. If names are similar, Hermes should ask which vehicle you mean. Maintenance history can be filtered by component and action, with up to 50 matching records returned newest first. “Last changed” means the latest recorded replacement. A missing record means Pulse has no matching entry.

Maintenance records need a title, a completed service date, and an amount. You can include the odometer, provider, notes, and multiple actions. The available categories are **service, repair, insurance, washing, and tyres**. Actions include **replaced, inspected, adjusted, repaired, topped up, cleaned, lubricated, and completed**. An oil change is **engine oil replaced**, and a full service is **general service completed**. If you paste invoice text, identify completed work and confirm the extracted entry before saving. Upload invoice attachments in Pulse web or Android.

Reminder tasks are **engine oil, general service, and tyre pressure**. Hermes should read the current setup first and show both saved schedules before changing either one. Select **Personal** or **Manual** as the active source. Each schedule uses kilometres and whole calendar months, with zero disabling that threshold. Either active threshold can make work due. The Manual source uses the values and reference you provide. Coming Up shows the effective intervals, and Pulse's existing automatic notifications apply to tyre pressure.

## Go API used by Hermes

`POST /api/gym-visits/{id}/exercises/batch` appends exercises and can replace the visit classification in the same transaction. It uses the authenticated Pulse account and the same workout records as the app.

```json
{
  "workout_type": "single_muscle",
  "workout_focus": "Shoulders",
  "exercises": [{
    "name": "weight plate front raises",
    "exercise_catalog_id": "Front_Plate_Raise",
    "remember_alias": true,
    "sets": [
      {"reps": 10, "weight": 10},
      {"reps": 10, "weight": 10},
      {"reps": 10, "weight": 10}
    ]
  }]
}
```

| Request | Result |
| --- | --- |
| Exercises only | `201` with saved exercises. |
| Exercises plus type and focus | `201` with saved exercises. The classification, sets, and requested aliases commit together. |
| Type and focus only | `200` with `[]`. |
| Type with an empty or omitted focus | Saves the specified type and clears the focus. |
| Empty type and empty or omitted focus | Clears both classification fields. |
| Both classification fields omitted | Keeps the saved classification. |

Type keys are `single_muscle`, `double_muscle`, `push`, `pull`, `legs`, `upper_body`, `lower_body`, `full_body`, `cardio`, `mobility`, and `other`.

For setup and tests, see [Pulse MCP server](README.md).
