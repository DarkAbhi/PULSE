# User time zones

The account preference defaults to **UTC** for existing and new users. The profile stores an IANA zone identifier, such as `America/New_York`, so daylight-saving rules apply automatically.

## API and database

Apply the normal Go migration workflow through migration `00025_profile_timezone`. It adds `user_profiles.timezone TEXT NOT NULL DEFAULT 'UTC'`; the down migration drops the preference. Existing timestamp values stay unchanged.

`GET /api/profile` always returns `timezone`, including when `has_profile` is false. `PUT /api/profile/timezone` accepts `{ "timezone": "America/New_York" }` and returns the saved `timezone`. The endpoint requires the authenticated account, validates the zone against the bundled time zone database, and returns 400 for invalid identifiers. Setting a zone does not complete display-name onboarding. Existing name updates preserve the saved zone.

Fitness today, workout calendar grouping and day queries use the account zone. Day bounds use the next calendar midnight, including 23- and 25-hour daylight-saving days. The next-month purchase period and upcoming subscription renewal calculations also follow the preference. Timestamps remain absolute instants; calendar-only values remain dates.

## Android

Profile includes a searchable time zone selector. The preference is loaded before authenticated screens open, refreshed from Profile, and cleared to UTC at sign-out. Saving it updates the active app preference and refreshes affected screens. Dashboard, notifications, API-key dates, fitness, garage and Horizon use the account zone. Date/time entry converts wall-clock values in that zone back to instants. Garage and Horizon drafts retain their original zone through process recreation. Nonexistent daylight-saving entry times are rejected; unchanged repeated-hour timestamps retain their original instant.

Meal plans and other calendar-only values retain their dates and local meal times. The selected zone controls the meaning of today. Material date-picker coordinates remain UTC as required by the picker; they are not timestamps to convert.

Time zone explanatory notices have been removed from Android and web. Deploy the migrated Go API before updated clients.

## Web

Profile includes a searchable selector using the same account preference and update endpoint as Android. UTC is the default. The server loads the preference per request, and the browser shares it through a React context. Saving invalidates the layout and refreshes server-rendered data. Navigation and returning to the browser refresh the preference, including changes made in Android. Sign-out clears the browser preference; it is not stored in localStorage or shared between users.

Dashboard clocks, notifications, API-key metadata, release/build timestamps, workout details, garage history and Horizon timestamps use the selected zone. Fuel, maintenance, transaction and statement-import forms convert entered wall times to UTC. An unchanged timestamp retains its original precision and repeated-hour offset; nonexistent local times are rejected. New repeated-hour entries use the earlier occurrence, matching Android. Open date/time forms retain the zone in which the draft was created.

Meal-plan and annual-renewal dates stay calendar-only values. Meal calendar arithmetic uses UTC coordinates to avoid the browser's zone shifting a calendar date, while today follows the account zone. Subscription day counts use calendar-day differences rather than assuming every day lasts 24 hours. A statement transaction with a date but no time is recorded at noon in the selected zone; explicit offsets are respected.

Validation: `npm test`, TypeScript checking, and the production build pass in the Linux web toolchain. Browser checks with isolated accounts verify searching, saving, persistence after reload, date changes across midnight, account isolation, and a New York noon transaction saved as 16:00 UTC. The Windows Turbopack worker failed to start in this environment, so the production build was verified in Docker instead.
