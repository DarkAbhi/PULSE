-- Anonymized development snapshot; regenerate with make dev-seed-export.
-- Data values are fictional. Relationships and relative dates follow the source.
\set ON_ERROR_STOP on
SELECT :'seed_environment' IN ('development', 'test') AS allowed \gset
\if :allowed
\else
DO $$ BEGIN RAISE EXCEPTION 'Development seed requires seed_environment=development or test'; END $$;
\endif
BEGIN;
SET LOCAL lock_timeout = '10s';
LOCK TABLE public.api_keys, public.credit_cards, public.exercise_aliases, public.exercise_catalog, public.financial_horizon_budgets, public.financial_horizon_categories, public.financial_horizon_configs, public.financial_horizon_deductions, public.financial_horizon_subscriptions, public.financial_horizon_transactions, public.fitness_activity_rings, public.fitness_workouts, public.gym_exercise_sets, public.gym_visit_exercises, public.gym_visits, public.meal_plans, public.meal_times, public.meditations, public.next_month_purchases, public.notifications, public.sports, public.user_profiles, public.user_sessions, public.users, public.vehicle_air_fills, public.vehicle_fuel_fillups, public.vehicle_fuel_items, public.vehicle_maintenance_attachments, public.vehicle_maintenance_records, public.vehicles, public.workout_activity_types IN SHARE ROW EXCLUSIVE MODE;
SELECT NOT EXISTS (SELECT FROM public.api_keys) AND NOT EXISTS (SELECT FROM public.credit_cards) AND NOT EXISTS (SELECT FROM public.exercise_aliases) AND NOT EXISTS (SELECT FROM public.financial_horizon_budgets) AND NOT EXISTS (SELECT FROM public.financial_horizon_configs) AND NOT EXISTS (SELECT FROM public.financial_horizon_deductions) AND NOT EXISTS (SELECT FROM public.financial_horizon_subscriptions) AND NOT EXISTS (SELECT FROM public.financial_horizon_transactions) AND NOT EXISTS (SELECT FROM public.fitness_activity_rings) AND NOT EXISTS (SELECT FROM public.fitness_workouts) AND NOT EXISTS (SELECT FROM public.gym_exercise_sets) AND NOT EXISTS (SELECT FROM public.gym_visit_exercises) AND NOT EXISTS (SELECT FROM public.gym_visits) AND NOT EXISTS (SELECT FROM public.meal_plans) AND NOT EXISTS (SELECT FROM public.meditations) AND NOT EXISTS (SELECT FROM public.next_month_purchases) AND NOT EXISTS (SELECT FROM public.notifications) AND NOT EXISTS (SELECT FROM public.sports) AND NOT EXISTS (SELECT FROM public.user_profiles) AND NOT EXISTS (SELECT FROM public.user_sessions) AND NOT EXISTS (SELECT FROM public.vehicle_air_fills) AND NOT EXISTS (SELECT FROM public.vehicle_fuel_fillups) AND NOT EXISTS (SELECT FROM public.vehicle_fuel_items) AND NOT EXISTS (SELECT FROM public.vehicle_maintenance_attachments) AND NOT EXISTS (SELECT FROM public.vehicle_maintenance_records) AND NOT EXISTS (SELECT FROM public.vehicles) AND NOT EXISTS (SELECT FROM public.workout_activity_types) AND NOT EXISTS (SELECT FROM users WHERE username <> 'admin' OR password_hash <> '$2y$12$bB7WwVq7nGJ4cfNTCX6kQODcNRLQvMjRhIFuH4Qv2.GAxlqNac4/S') AND (SELECT count(*) FROM users) = 1 AND NOT EXISTS (SELECT FROM financial_horizon_categories WHERE user_id IS NOT NULL) AND NOT EXISTS (SELECT FROM meal_times WHERE user_id IS NOT NULL) AS seed_empty \gset
\if :seed_empty
\else
\echo Database already populated; development seed skipped.
COMMIT;
\quit
\endif
DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM schema_migrations WHERE version = 23 AND NOT dirty) THEN
        RAISE EXCEPTION 'Seed schema version mismatch; regenerate the development seed';
    END IF;
END $$;
TRUNCATE public.api_keys, public.credit_cards, public.exercise_aliases, public.financial_horizon_budgets, public.financial_horizon_categories, public.financial_horizon_configs, public.financial_horizon_deductions, public.financial_horizon_subscriptions, public.financial_horizon_transactions, public.fitness_activity_rings, public.fitness_workouts, public.gym_exercise_sets, public.gym_visit_exercises, public.gym_visits, public.meal_plans, public.meal_times, public.meditations, public.next_month_purchases, public.notifications, public.sports, public.user_profiles, public.user_sessions, public.users, public.vehicle_air_fills, public.vehicle_fuel_fillups, public.vehicle_fuel_items, public.vehicle_maintenance_attachments, public.vehicle_maintenance_records, public.vehicles, public.workout_activity_types RESTART IDENTITY;
COPY public.credit_cards (id, name, link, created_at, updated_at) FROM stdin;
\.
SELECT setval(pg_get_serial_sequence('public.credit_cards', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.credit_cards;
COPY public.users (id, username, password_hash, created_at, updated_at) FROM stdin;
1	admin	$2y$12$bB7WwVq7nGJ4cfNTCX6kQODcNRLQvMjRhIFuH4Qv2.GAxlqNac4/S	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.users', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.users;
COPY public.workout_activity_types (id, raw_value, name, created_at) FROM stdin;
1	52	Walking	2025-11-26 08:00:00+05:30
2	37	Running	2025-11-26 08:00:00+05:30
347	13	Cycling	2026-01-03 08:00:00+05:30
3	50	Traditional Strength Training	2025-11-26 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.workout_activity_types', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.workout_activity_types;
COPY public.meditations (id, created_at, updated_at) FROM stdin;
1	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.meditations', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.meditations;
COPY public.sports (id, name, created_at, updated_at) FROM stdin;
1	cricket	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
2	football	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
3	badminton	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.sports', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.sports;
COPY public.exercise_aliases (user_id, normalized_alias, exercise_catalog_id) FROM stdin;
\.
COPY public.financial_horizon_budgets (id, user_id, name, allocated_amount, created_at, updated_at) FROM stdin;
1	1	Demo name 1	5000	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.financial_horizon_budgets', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.financial_horizon_budgets;
COPY public.financial_horizon_categories (id, user_id, name, icon, color, is_default, created_at, updated_at) FROM stdin;
10	\N	Demo name 10	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
1	\N	Demo name 1	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
2	\N	Demo name 2	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
3	\N	Demo name 3	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
4	\N	Demo name 4	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
5	\N	Demo name 5	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
6	\N	Demo name 6	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
7	\N	Demo name 7	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
8	\N	Demo name 8	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
9	\N	Demo name 9	tag	#64748b	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.financial_horizon_categories', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.financial_horizon_categories;
COPY public.financial_horizon_configs (user_id, base_amount, currency, created_at, updated_at) FROM stdin;
1	50000	INR	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30
\.
COPY public.fitness_activity_rings (id, summary_date, move_calories, move_calories_goal, exercise_minutes, exercise_minutes_goal, stand_hours, stand_hours_goal, steps_count, created_at, updated_at, user_id) FROM stdin;
103	2025-12-01	200	350	30	40	8	12	6000	2025-12-01 08:00:00+05:30	2025-12-01 08:00:00+05:30	1
105	2025-12-02	200	350	30	40	8	12	6000	2025-12-02 08:00:00+05:30	2025-12-02 08:00:00+05:30	1
106	2025-12-03	200	350	30	40	8	12	6000	2025-12-03 08:00:00+05:30	2025-12-03 08:00:00+05:30	1
112	2025-12-04	200	350	30	40	8	12	6000	2025-12-04 08:00:00+05:30	2025-12-04 08:00:00+05:30	1
131	2025-12-05	200	350	30	40	8	12	6000	2025-12-05 08:00:00+05:30	2025-12-05 08:00:00+05:30	1
141	2025-12-06	200	350	30	40	8	12	6000	2025-12-06 08:00:00+05:30	2025-12-06 08:00:00+05:30	1
158	2025-12-07	200	350	30	40	8	12	6000	2025-12-07 08:00:00+05:30	2025-12-07 08:00:00+05:30	1
180	2025-12-08	200	350	30	40	8	12	6000	2025-12-08 08:00:00+05:30	2025-12-08 08:00:00+05:30	1
1	2025-11-26	200	350	30	40	8	12	6000	2025-11-26 08:00:00+05:30	2025-11-26 08:00:00+05:30	1
203	2025-12-09	200	350	30	40	8	12	6000	2025-12-09 08:00:00+05:30	2025-12-09 08:00:00+05:30	1
213	2025-12-10	200	350	30	40	8	12	6000	2025-12-10 08:00:00+05:30	2025-12-10 08:00:00+05:30	1
214	2025-12-11	200	350	30	40	8	12	6000	2025-12-11 08:00:00+05:30	2025-12-11 08:00:00+05:30	1
217	2025-12-12	200	350	30	40	8	12	6000	2025-12-12 08:00:00+05:30	2025-12-12 08:00:00+05:30	1
218	2025-12-15	200	350	30	40	8	12	6000	2025-12-15 08:00:00+05:30	2025-12-15 08:00:00+05:30	1
219	2025-12-18	200	350	30	40	8	12	6000	2025-12-18 08:00:00+05:30	2025-12-18 08:00:00+05:30	1
220	2025-12-21	200	350	30	40	8	12	6000	2025-12-21 08:00:00+05:30	2025-12-21 08:00:00+05:30	1
221	2025-12-22	200	350	30	40	8	12	6000	2025-12-22 08:00:00+05:30	2025-12-22 08:00:00+05:30	1
222	2025-12-23	200	350	30	40	8	12	6000	2025-12-23 08:00:00+05:30	2025-12-23 08:00:00+05:30	1
224	2025-12-24	200	350	30	40	8	12	6000	2025-12-24 08:00:00+05:30	2025-12-24 08:00:00+05:30	1
225	2025-12-25	200	350	30	40	8	12	6000	2025-12-25 08:00:00+05:30	2025-12-25 08:00:00+05:30	1
227	2025-12-26	200	350	30	40	8	12	6000	2025-12-26 08:00:00+05:30	2025-12-26 08:00:00+05:30	1
228	2025-12-27	200	350	30	40	8	12	6000	2025-12-27 08:00:00+05:30	2025-12-27 08:00:00+05:30	1
229	2025-12-28	200	350	30	40	8	12	6000	2025-12-28 08:00:00+05:30	2025-12-28 08:00:00+05:30	1
232	2025-12-29	200	350	30	40	8	12	6000	2025-12-29 08:00:00+05:30	2025-12-29 08:00:00+05:30	1
238	2025-12-30	200	350	30	40	8	12	6000	2025-12-30 08:00:00+05:30	2025-12-30 08:00:00+05:30	1
261	2025-12-31	200	350	30	40	8	12	6000	2025-12-31 08:00:00+05:30	2025-12-31 08:00:00+05:30	1
286	2026-01-01	200	350	30	40	8	12	6000	2026-01-01 08:00:00+05:30	2026-01-01 08:00:00+05:30	1
306	2026-01-02	200	350	30	40	8	12	6000	2026-01-02 08:00:00+05:30	2026-01-02 08:00:00+05:30	1
334	2026-01-03	200	350	30	40	8	12	6000	2026-01-03 08:00:00+05:30	2026-01-03 08:00:00+05:30	1
358	2026-01-04	200	350	30	40	8	12	6000	2026-01-04 08:00:00+05:30	2026-01-04 08:00:00+05:30	1
366	2026-01-05	200	350	30	40	8	12	6000	2026-01-05 08:00:00+05:30	2026-01-05 08:00:00+05:30	1
392	2026-01-08	200	350	30	40	8	12	6000	2026-01-08 08:00:00+05:30	2026-01-08 08:00:00+05:30	1
394	2026-01-09	200	350	30	40	8	12	6000	2026-01-09 08:00:00+05:30	2026-01-09 08:00:00+05:30	1
396	2026-01-11	200	350	30	40	8	12	6000	2026-01-11 08:00:00+05:30	2026-01-11 08:00:00+05:30	1
401	2026-01-12	200	350	30	40	8	12	6000	2026-01-12 08:00:00+05:30	2026-01-12 08:00:00+05:30	1
403	2026-01-13	200	350	30	40	8	12	6000	2026-01-13 08:00:00+05:30	2026-01-13 08:00:00+05:30	1
404	2026-01-15	200	350	30	40	8	12	6000	2026-01-15 08:00:00+05:30	2026-01-15 08:00:00+05:30	1
406	2026-01-16	200	350	30	40	8	12	6000	2026-01-16 08:00:00+05:30	2026-01-16 08:00:00+05:30	1
408	2026-01-17	200	350	30	40	8	12	6000	2026-01-17 08:00:00+05:30	2026-01-17 08:00:00+05:30	1
410	2026-01-18	200	350	30	40	8	12	6000	2026-01-18 08:00:00+05:30	2026-01-18 08:00:00+05:30	1
411	2026-01-20	200	350	30	40	8	12	6000	2026-01-20 08:00:00+05:30	2026-01-20 08:00:00+05:30	1
412	2026-01-21	200	350	30	40	8	12	6000	2026-01-21 08:00:00+05:30	2026-01-21 08:00:00+05:30	1
413	2026-01-22	200	350	30	40	8	12	6000	2026-01-22 08:00:00+05:30	2026-01-22 08:00:00+05:30	1
430	2026-01-23	200	350	30	40	8	12	6000	2026-01-23 08:00:00+05:30	2026-01-23 08:00:00+05:30	1
450	2026-01-24	200	350	30	40	8	12	6000	2026-01-24 08:00:00+05:30	2026-01-24 08:00:00+05:30	1
471	2026-01-25	200	350	30	40	8	12	6000	2026-01-25 08:00:00+05:30	2026-01-25 08:00:00+05:30	1
492	2026-01-26	200	350	30	40	8	12	6000	2026-01-26 08:00:00+05:30	2026-01-26 08:00:00+05:30	1
501	2026-01-27	200	350	30	40	8	12	6000	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30	1
515	2026-01-28	200	350	30	40	8	12	6000	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30	1
51	2025-11-27	200	350	30	40	8	12	6000	2025-11-27 08:00:00+05:30	2025-11-27 08:00:00+05:30	1
524	2026-01-29	200	350	30	40	8	12	6000	2026-01-29 08:00:00+05:30	2026-01-29 08:00:00+05:30	1
542	2026-01-30	200	350	30	40	8	12	6000	2026-01-30 08:00:00+05:30	2026-01-30 08:00:00+05:30	1
82	2025-11-28	200	350	30	40	8	12	6000	2025-11-28 08:00:00+05:30	2025-11-28 08:00:00+05:30	1
91	2025-11-29	200	350	30	40	8	12	6000	2025-11-29 08:00:00+05:30	2025-11-29 08:00:00+05:30	1
97	2025-11-30	200	350	30	40	8	12	6000	2025-11-30 08:00:00+05:30	2025-11-30 08:00:00+05:30	1
\.
SELECT setval(pg_get_serial_sequence('public.fitness_activity_rings', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.fitness_activity_rings;
COPY public.meal_times (id, user_id, name, start_time, end_time, is_default, created_at, updated_at) FROM stdin;
1	\N	Demo name 1	08:00:00	09:00:00	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
2	\N	Demo name 2	08:00:00	09:00:00	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
3	\N	Demo name 3	08:00:00	09:00:00	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
4	\N	Demo name 4	08:00:00	09:00:00	true	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.meal_times', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.meal_times;
COPY public.next_month_purchases (id, user_id, target_month, name, price, url, created_at, updated_at) FROM stdin;
\.
SELECT setval(pg_get_serial_sequence('public.next_month_purchases', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.next_month_purchases;
COPY public.notifications (id, user_id, source, title, body, target_path, priority, metadata, read_at, dismissed_at, created_at) FROM stdin;
\.
SELECT setval(pg_get_serial_sequence('public.notifications', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.notifications;
COPY public.user_profiles (user_id, display_name, created_at, updated_at) FROM stdin;
1	Demo display name 1	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30
\.
COPY public.vehicles (id, name, is_active, created_at, updated_at, front_tire_pressure_solo, rear_tire_pressure_solo, front_tire_pressure_pillion, rear_tire_pressure_pillion, user_id) FROM stdin;
1	Demo name 1	true	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30	32	32	32	32	1
2	Demo name 2	true	2026-01-29 08:00:00+05:30	2026-01-29 08:00:00+05:30	\N	\N	\N	\N	1
\.
SELECT setval(pg_get_serial_sequence('public.vehicles', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.vehicles;
COPY public.fitness_workouts (id, uuid, activity_type_id, start_time, end_time, duration_seconds, calories_burned, distance_meters, metadata, created_at, updated_at, user_id) FROM stdin;
121	ac2b775c-43b8-5662-98a5-9660294d4525	1	2025-11-26 08:00:00+05:30	2025-11-26 08:30:00+05:30	1800	200	5000	{}	2025-11-26 08:00:00+05:30	2025-11-26 08:00:00+05:30	1
146	f8878e2d-415f-57e1-a91e-b47642dc8b28	1	2025-11-28 08:00:00+05:30	2025-11-28 08:30:00+05:30	1800	200	5000	{}	2025-11-28 08:00:00+05:30	2025-11-28 08:00:00+05:30	1
147	37ab3845-c335-5192-aa61-ca9180407032	2	2025-11-28 08:00:00+05:30	2025-11-28 08:30:00+05:30	1800	200	5000	{}	2025-11-28 08:00:00+05:30	2025-11-28 08:00:00+05:30	1
150	48ed95b0-d73a-5b38-bc8b-b757551101d8	3	2025-11-28 08:00:00+05:30	2025-11-28 08:30:00+05:30	1800	200	\N	{}	2025-11-28 08:00:00+05:30	2025-11-28 08:00:00+05:30	1
166	c81c9eec-701f-599c-b5f0-bea6a28366cb	1	2025-11-29 08:00:00+05:30	2025-11-29 08:30:00+05:30	1800	200	5000	{}	2025-11-29 08:00:00+05:30	2025-11-29 08:00:00+05:30	1
167	3e413715-280e-5532-bbb7-542bd1450bfb	2	2025-11-29 08:00:00+05:30	2025-11-29 08:30:00+05:30	1800	200	5000	{}	2025-11-29 08:00:00+05:30	2025-11-29 08:00:00+05:30	1
172	de80cb66-95b6-5a4c-9c12-f045d74ab910	3	2025-11-29 08:00:00+05:30	2025-11-29 08:30:00+05:30	1800	200	\N	{}	2025-11-29 08:00:00+05:30	2025-11-29 08:00:00+05:30	1
176	cc125233-d6e7-5574-969c-609b015bf3c2	3	2025-12-04 08:00:00+05:30	2025-12-04 08:30:00+05:30	1800	200	\N	{}	2025-12-04 08:00:00+05:30	2025-12-04 08:00:00+05:30	1
178	1346a4b2-d298-5707-92af-cec33942ace1	2	2025-12-04 08:00:00+05:30	2025-12-04 08:30:00+05:30	1800	200	5000	{}	2025-12-04 08:00:00+05:30	2025-12-04 08:00:00+05:30	1
199	819358fb-f6be-5c7c-8318-542d14dbbd85	1	2025-12-06 08:00:00+05:30	2025-12-06 08:30:00+05:30	1800	200	5000	{}	2025-12-06 08:00:00+05:30	2025-12-06 08:00:00+05:30	1
1	3f417c7d-dfd7-52e9-9860-ae69507f09d7	1	2025-11-26 08:00:00+05:30	2025-11-26 08:30:00+05:30	1800	200	5000	{}	2025-11-26 08:00:00+05:30	2025-11-26 08:00:00+05:30	1
200	0840ad5e-dca7-5be7-ae51-9e9d17bda1ab	2	2025-12-06 08:00:00+05:30	2025-12-06 08:30:00+05:30	1800	200	5000	{}	2025-12-06 08:00:00+05:30	2025-12-06 08:00:00+05:30	1
205	770a3164-6705-5608-8637-89588b9266db	3	2025-12-06 08:00:00+05:30	2025-12-06 08:30:00+05:30	1800	200	\N	{}	2025-12-06 08:00:00+05:30	2025-12-06 08:00:00+05:30	1
224	7d945e91-3f00-51cb-97a4-7a5985e90705	1	2025-12-07 08:00:00+05:30	2025-12-07 08:30:00+05:30	1800	200	5000	{}	2025-12-07 08:00:00+05:30	2025-12-07 08:00:00+05:30	1
225	098b4446-fddf-5a9d-bc0e-0856ad57472c	2	2025-12-07 08:00:00+05:30	2025-12-07 08:30:00+05:30	1800	200	5000	{}	2025-12-07 08:00:00+05:30	2025-12-07 08:00:00+05:30	1
230	3be69aa7-08e8-55ef-a6b1-edec13331bac	3	2025-12-07 08:00:00+05:30	2025-12-07 08:30:00+05:30	1800	200	\N	{}	2025-12-07 08:00:00+05:30	2025-12-07 08:00:00+05:30	1
240	4aca3c64-f538-5f5c-8222-b0f3d0095a7c	1	2025-12-29 08:00:00+05:30	2025-12-29 08:30:00+05:30	1800	200	5000	{}	2025-12-29 08:00:00+05:30	2025-12-29 08:00:00+05:30	1
241	97dabd3e-83d4-52a4-be97-5262d1908592	2	2025-12-29 08:00:00+05:30	2025-12-29 08:30:00+05:30	1800	200	5000	{}	2025-12-29 08:00:00+05:30	2025-12-29 08:00:00+05:30	1
242	7358fc86-ec5f-5a73-8b9a-a66799069366	3	2025-12-29 08:00:00+05:30	2025-12-29 08:30:00+05:30	1800	200	\N	{}	2025-12-29 08:00:00+05:30	2025-12-29 08:00:00+05:30	1
255	1dd67d28-5a41-500a-a589-f15cb6f233ac	1	2025-12-31 08:00:00+05:30	2025-12-31 08:30:00+05:30	1800	200	5000	{}	2025-12-31 08:00:00+05:30	2025-12-31 08:00:00+05:30	1
256	8c14513c-13ef-5b9b-8744-eae37aec75eb	2	2025-12-31 08:00:00+05:30	2025-12-31 08:30:00+05:30	1800	200	5000	{}	2025-12-31 08:00:00+05:30	2025-12-31 08:00:00+05:30	1
257	0d64a769-eb42-5393-8914-af336ef69fb4	3	2025-12-31 08:00:00+05:30	2025-12-31 08:30:00+05:30	1800	200	\N	{}	2025-12-31 08:00:00+05:30	2025-12-31 08:00:00+05:30	1
273	1c9e2d83-7421-52f6-afe9-a126efa3faad	1	2025-12-31 08:00:00+05:30	2025-12-31 08:30:00+05:30	1800	200	5000	{}	2025-12-31 08:00:00+05:30	2025-12-31 08:00:00+05:30	1
282	bc03f7fa-15e3-5215-9533-3afd82a450ad	1	2026-01-01 08:00:00+05:30	2026-01-01 08:30:00+05:30	1800	200	5000	{}	2026-01-01 08:00:00+05:30	2026-01-01 08:00:00+05:30	1
283	29886992-cc31-5f15-b6c0-e63773a7b754	2	2026-01-01 08:00:00+05:30	2026-01-01 08:30:00+05:30	1800	200	5000	{}	2026-01-01 08:00:00+05:30	2026-01-01 08:00:00+05:30	1
284	05e439a7-1766-5184-97d5-2fc6c0248847	3	2026-01-01 08:00:00+05:30	2026-01-01 08:30:00+05:30	1800	200	\N	{}	2026-01-01 08:00:00+05:30	2026-01-01 08:00:00+05:30	1
2	8a29596d-7f54-52e8-a5af-452c172098be	2	2025-11-26 08:00:00+05:30	2025-11-26 08:30:00+05:30	1800	200	5000	{}	2025-11-26 08:00:00+05:30	2025-11-26 08:00:00+05:30	1
318	82f76f61-a528-5cb0-bf40-414a0b5012f9	1	2026-01-02 08:00:00+05:30	2026-01-02 08:30:00+05:30	1800	200	5000	{}	2026-01-02 08:00:00+05:30	2026-01-02 08:00:00+05:30	1
321	fb71b4e2-376b-587b-bfa4-be048874aa9d	2	2026-01-02 08:00:00+05:30	2026-01-02 08:30:00+05:30	1800	200	5000	{}	2026-01-02 08:00:00+05:30	2026-01-02 08:00:00+05:30	1
326	17ba779e-6f80-576e-a756-a09239e77351	3	2026-01-02 08:00:00+05:30	2026-01-02 08:30:00+05:30	1800	200	\N	{}	2026-01-02 08:00:00+05:30	2026-01-02 08:00:00+05:30	1
357	a78dfbe6-a091-57b4-b0c7-ee1eebac0ad5	347	2026-01-03 08:00:00+05:30	2026-01-03 08:30:00+05:30	1800	200	5000	{}	2026-01-03 08:00:00+05:30	2026-01-03 08:00:00+05:30	1
359	ec28a2b2-be0a-5d8e-a3b7-ca14a05c682c	347	2026-01-03 08:00:00+05:30	2026-01-03 08:30:00+05:30	1800	200	5000	{}	2026-01-03 08:00:00+05:30	2026-01-03 08:00:00+05:30	1
366	77dbe6ca-b2bf-5de9-a317-d815a1f280f6	1	2026-01-05 08:00:00+05:30	2026-01-05 08:30:00+05:30	1800	200	5000	{}	2026-01-05 08:00:00+05:30	2026-01-05 08:00:00+05:30	1
367	240ff642-2946-585c-a5f2-dfae0327e9a8	2	2026-01-05 08:00:00+05:30	2026-01-05 08:30:00+05:30	1800	200	5000	{}	2026-01-05 08:00:00+05:30	2026-01-05 08:00:00+05:30	1
372	34f5a546-feb1-5a82-8950-da4b3b43d3b4	3	2026-01-05 08:00:00+05:30	2026-01-05 08:30:00+05:30	1800	200	\N	{}	2026-01-05 08:00:00+05:30	2026-01-05 08:00:00+05:30	1
391	5046acfb-a69c-5fb3-991b-1c835c3d4b65	1	2026-01-08 08:00:00+05:30	2026-01-08 08:30:00+05:30	1800	200	5000	{}	2026-01-08 08:00:00+05:30	2026-01-08 08:00:00+05:30	1
392	7283c87d-0299-511e-9cc9-77a52436063e	3	2026-01-08 08:00:00+05:30	2026-01-08 08:30:00+05:30	1800	200	\N	{}	2026-01-08 08:00:00+05:30	2026-01-08 08:00:00+05:30	1
393	4f16eb27-3bc5-52e3-9ce2-6007663e549b	347	2026-01-08 08:00:00+05:30	2026-01-08 08:30:00+05:30	1800	200	5000	{}	2026-01-08 08:00:00+05:30	2026-01-08 08:00:00+05:30	1
397	32eccfd9-f947-5a58-a83a-0cdfbb96301d	1	2026-01-22 08:00:00+05:30	2026-01-22 08:30:00+05:30	1800	200	5000	{}	2026-01-22 08:00:00+05:30	2026-01-22 08:00:00+05:30	1
399	4ba14c32-7222-5696-ba3f-b4aa7ff75138	2	2026-01-22 08:00:00+05:30	2026-01-22 08:30:00+05:30	1800	200	5000	{}	2026-01-22 08:00:00+05:30	2026-01-22 08:00:00+05:30	1
3	705e79c8-f4be-5753-bf1b-c87694edc01b	3	2025-11-26 08:00:00+05:30	2025-11-26 08:30:00+05:30	1800	200	\N	{}	2025-11-26 08:00:00+05:30	2025-11-26 08:00:00+05:30	1
406	c51da5c8-56f8-54f0-abb1-6ff83349c70a	3	2026-01-22 08:00:00+05:30	2026-01-22 08:30:00+05:30	1800	200	\N	{}	2026-01-22 08:00:00+05:30	2026-01-22 08:00:00+05:30	1
431	24fbb779-0921-5c51-8851-c425b2d05889	347	2026-01-22 08:00:00+05:30	2026-01-22 08:30:00+05:30	1800	200	5000	{}	2026-01-22 08:00:00+05:30	2026-01-22 08:00:00+05:30	1
440	4a1b1a47-ac9a-5025-b7f9-374c7d1f39c5	1	2026-01-23 08:00:00+05:30	2026-01-23 08:30:00+05:30	1800	200	5000	{}	2026-01-23 08:00:00+05:30	2026-01-23 08:00:00+05:30	1
442	e54fce9e-2b0c-5fba-8d5e-c48361365db6	2	2026-01-23 08:00:00+05:30	2026-01-23 08:30:00+05:30	1800	200	5000	{}	2026-01-23 08:00:00+05:30	2026-01-23 08:00:00+05:30	1
449	b166c920-ab49-569d-aa02-a615a1ae6aa7	3	2026-01-23 08:00:00+05:30	2026-01-23 08:30:00+05:30	1800	200	\N	{}	2026-01-23 08:00:00+05:30	2026-01-23 08:00:00+05:30	1
471	6eefbd6a-f76d-5a45-af76-96949a8d03dd	3	2026-01-29 08:00:00+05:30	2026-01-29 08:30:00+05:30	1800	200	\N	{}	2026-01-29 08:00:00+05:30	2026-01-29 08:00:00+05:30	1
472	22108c8e-a1b8-55b4-8cca-69299245dba5	1	2026-01-29 08:00:00+05:30	2026-01-29 08:30:00+05:30	1800	200	5000	{}	2026-01-29 08:00:00+05:30	2026-01-29 08:00:00+05:30	1
473	eee19b9c-7a44-5d8f-95e8-7410662c2a52	2	2026-01-29 08:00:00+05:30	2026-01-29 08:30:00+05:30	1800	200	5000	{}	2026-01-29 08:00:00+05:30	2026-01-29 08:00:00+05:30	1
\.
SELECT setval(pg_get_serial_sequence('public.fitness_workouts', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.fitness_workouts;
COPY public.financial_horizon_deductions (id, user_id, name, category, amount, due_day, is_active, created_at, updated_at, budget_id) FROM stdin;
\.
SELECT setval(pg_get_serial_sequence('public.financial_horizon_deductions', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.financial_horizon_deductions;
COPY public.meal_plans (id, user_id, date, name, meal_time_id, start_time, end_time, created_at, updated_at, is_consumed) FROM stdin;
1	1	2026-01-28	Demo name 1	1	08:00:00	09:00:00	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30	true
2	1	2026-01-28	Demo name 2	3	08:00:00	09:00:00	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30	false
\.
SELECT setval(pg_get_serial_sequence('public.meal_plans', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.meal_plans;
COPY public.vehicle_air_fills (id, vehicle_id, user_id, filled_at, reminder_notification_id, created_at) FROM stdin;
1	1	1	2026-01-28 08:00:00+05:30	\N	2026-01-28 08:00:00+05:30
2	2	1	2026-01-29 08:00:00+05:30	\N	2026-01-29 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.vehicle_air_fills', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.vehicle_air_fills;
COPY public.vehicle_fuel_fillups (id, vehicle_id, user_id, odometer_km, filled_at, station_name, notes, created_at) FROM stdin;
1	1	1	1000	2026-01-28 08:00:00+05:30	\N	\N	2026-01-28 08:00:00+05:30
2	1	1	1000	2026-02-04 08:00:00+05:30	\N	\N	2026-02-04 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.vehicle_fuel_fillups', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.vehicle_fuel_fillups;
COPY public.vehicle_maintenance_records (id, vehicle_id, user_id, category, title, amount, occurred_at, odometer_km, provider_name, notes, created_at, updated_at) FROM stdin;
1	1	1	service	Demo title 1	501	2026-01-28 08:00:00+05:30	1000	\N	\N	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.vehicle_maintenance_records', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.vehicle_maintenance_records;
COPY public.gym_visits (id, created_at, updated_at, user_id, fitness_workout_id) FROM stdin;
10	2025-12-31 08:00:00+05:30	2025-12-31 08:00:00+05:30	1	257
11	2026-01-01 08:00:00+05:30	2026-01-01 08:00:00+05:30	1	284
12	2026-01-02 08:00:00+05:30	2026-01-02 08:00:00+05:30	1	326
13	2026-01-05 08:00:00+05:30	2026-01-05 08:00:00+05:30	1	372
14	2026-01-08 08:00:00+05:30	2026-01-08 08:00:00+05:30	1	392
15	2026-01-22 08:00:00+05:30	2026-01-22 08:00:00+05:30	1	406
16	2026-01-23 08:00:00+05:30	2026-01-23 08:00:00+05:30	1	449
17	2026-01-29 08:00:00+05:30	2026-01-29 08:00:00+05:30	1	471
18	2026-01-31 08:00:00+05:30	2026-01-31 08:00:00+05:30	1	\N
19	2026-01-18 08:00:00+05:30	2026-02-01 08:00:00+05:30	1	\N
1	2026-01-27 08:00:00+05:30	2026-01-27 08:00:00+05:30	1	\N
2	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30	1	\N
3	2025-11-26 08:00:00+05:30	2025-11-26 08:00:00+05:30	1	3
4	2025-11-28 08:00:00+05:30	2025-11-28 08:00:00+05:30	1	150
5	2025-11-29 08:00:00+05:30	2025-11-29 08:00:00+05:30	1	172
6	2025-12-04 08:00:00+05:30	2025-12-04 08:00:00+05:30	1	176
7	2025-12-06 08:00:00+05:30	2025-12-06 08:00:00+05:30	1	205
8	2025-12-07 08:00:00+05:30	2025-12-07 08:00:00+05:30	1	230
9	2025-12-29 08:00:00+05:30	2025-12-29 08:00:00+05:30	1	242
\.
SELECT setval(pg_get_serial_sequence('public.gym_visits', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.gym_visits;
COPY public.financial_horizon_subscriptions (id, user_id, name, amount, billing_cycle, billing_day, renewal_date, status, category_id, budget_id, deduction_id, notes, created_at, updated_at) FROM stdin;
\.
SELECT setval(pg_get_serial_sequence('public.financial_horizon_subscriptions', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.financial_horizon_subscriptions;
COPY public.vehicle_fuel_items (id, fillup_id, fuel_type, fill_type, quantity, unit_price, total_cost, created_at) FROM stdin;
4	1	petrol	full	5	100	500	2026-02-04 08:00:00+05:30
5	2	petrol	full	5	100	500	2026-02-04 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.vehicle_fuel_items', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.vehicle_fuel_items;
COPY public.gym_visit_exercises (id, gym_visit_id, name, created_at, updated_at, exercise_catalog_id) FROM stdin;
1	2	Demo name 1	2026-01-28 08:00:00+05:30	2026-01-28 08:00:00+05:30	Barbell_Squat
3	2	Demo name 3	2026-02-03 08:00:00+05:30	2026-02-03 08:00:00+05:30	Front_Plate_Raise
\.
SELECT setval(pg_get_serial_sequence('public.gym_visit_exercises', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.gym_visit_exercises;
COPY public.financial_horizon_transactions (id, user_id, name, amount, transaction_date, category_id, category_name, budget_id, notes, created_at, updated_at, type, subscription_id) FROM stdin;
\.
SELECT setval(pg_get_serial_sequence('public.financial_horizon_transactions', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.financial_horizon_transactions;
COPY public.gym_exercise_sets (id, gym_visit_exercise_id, set_number, reps, weight, created_at) FROM stdin;
1	1	1	10	11	2026-01-28 08:00:00+05:30
2	1	2	10	12	2026-01-28 08:00:00+05:30
6	3	1	10	11	2026-02-03 08:00:00+05:30
\.
SELECT setval(pg_get_serial_sequence('public.gym_exercise_sets', 'id'), coalesce(max(id), 1), max(id) IS NOT NULL) FROM public.gym_exercise_sets;
UPDATE public.credit_cards SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.users SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.workout_activity_types SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.meditations SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.sports SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.financial_horizon_budgets SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.financial_horizon_categories SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.financial_horizon_configs SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.fitness_activity_rings SET summary_date = summary_date + (CURRENT_DATE - DATE '2026-01-31'), created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.meal_times SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.next_month_purchases SET target_month = date_trunc('month', CURRENT_DATE + interval '1 month')::date, created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.notifications SET read_at = read_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', dismissed_at = dismissed_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.user_profiles SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.vehicles SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.fitness_workouts SET start_time = start_time + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', end_time = end_time + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.financial_horizon_deductions SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.meal_plans SET date = date + (CURRENT_DATE - DATE '2026-01-31'), created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.vehicle_air_fills SET filled_at = filled_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.vehicle_fuel_fillups SET filled_at = filled_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.vehicle_maintenance_records SET occurred_at = occurred_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.gym_visits SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.financial_horizon_subscriptions SET renewal_date = renewal_date + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.vehicle_fuel_items SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.gym_visit_exercises SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.financial_horizon_transactions SET transaction_date = transaction_date + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day', updated_at = updated_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
UPDATE public.gym_exercise_sets SET created_at = created_at + (CURRENT_DATE - DATE '2026-01-31') * interval '1 day';
INSERT INTO user_profiles (user_id, display_name) VALUES (1, 'Demo Admin') ON CONFLICT (user_id) DO NOTHING;
\echo Applied anonymized development seed.
COMMIT;
