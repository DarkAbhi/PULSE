# User time zones

The account preference defaults to **UTC** for existing and new users. The profile stores an IANA zone identifier, such as `America/New_York`, so daylight-saving rules apply automatically.

## API and database

Apply the normal Go migration workflow through migration `00025_profile_timezone`. It adds `user_profiles.timezone TEXT NOT NULL DEFAULT 'UTC'`; the down migration drops the preference. Existing timestamp values stay unchanged.

`GET /api/profile` always returns `timezone`, including when `has_profile` is false. `PUT /api/profile/timezone` accepts `{ "timezone": "America/New_York" }` and returns the saved `timezone`. The endpoint requires the authenticated account, validates the zone against the bundled time zone database, and returns 400 for invalid identifiers. Setting a zone does not complete display-name onboarding. Existing name updates preserve the saved zone.

Fitness today, workout calendar grouping and day queries use the account zone. Day bounds use the next calendar midnight, including 23- and 25-hour daylight-saving days. The next-month purchase period and upcoming subscription renewal calculations also follow the preference. Timestamps remain absolute instants; calendar-only values remain dates.

## Android

Profile includes a searchable time zone selector. The preference is loaded before authenticated screens open, refreshed from Profile, and cleared to UTC at sign-out. Saving it updates the active app preference and refreshes affected screens. Dashboard, notifications, API-key dates, fitness, garage and Horizon use the account zone. Date/time entry converts wall-clock values in that zone back to instants. Garage and Horizon drafts retain their original zone through process recreation. Nonexistent daylight-saving entry times are rejected; unchanged repeated-hour timestamps retain their original instant.

Meal plans and other calendar-only values retain their dates and local meal times. The selected zone controls the meaning of today. Material date-picker coordinates remain UTC as required by the picker; they are not timestamps to convert.

Time zone explanatory notices have been removed from Android. Deploy the migrated Go API before the updated Android app. The web frontend has not been integrated in this change; its future integration should read and save the same profile preference and apply it when displaying timestamps.
