--
-- PostgreSQL database dump
--

\restrict utVrHJx1h7g9V21IXC41OTgjSXgdnOil8GYwiYkMUMQHvRN6wNdf3R9LZRidMbd

-- Dumped from database version 17.11
-- Dumped by pg_dump version 17.11

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: alarm_profile_versions; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.alarm_profile_versions VALUES ('01df5884-f45a-417c-a64d-5c37f2c89cff', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-04 10:41:07.11898', NULL, 'walker', false, 'custom', 'night_wandering', '{}', NULL, 'gaston', '2026-09-04 10:41:07.119328', 'medium', 0);


--
-- Data for Name: alarm_profile_overrides; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.alarm_profile_overrides VALUES ('1d8a1e06-3b0e-4d4a-b28b-61a832f3b637', '01df5884-f45a-417c-a64d-5c37f2c89cff', 'sleep_dwell', 'dwell', 'sleep_dwell', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-09-04 10:41:07.12132', NULL);


--
-- Data for Name: auth_sessions; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: bathroom_summaries; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.bathroom_summaries VALUES ('a5aa0e5c-c14f-4281-babd-9c4ecee8d9a8', 'seed-bath-r-jose-2024-02-27', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-21', 3, 0, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.896425', '2026-09-04 10:41:06.896209');
INSERT INTO public.bathroom_summaries VALUES ('81a32eb1-ca92-43cb-b618-3e91c8e57736', 'seed-bath-r-jose-2024-02-28', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-22', 5, 1, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.904694', '2026-09-04 10:41:06.904528');
INSERT INTO public.bathroom_summaries VALUES ('8a64dccd-09cf-4684-bcc8-9d4e6f3f4097', 'seed-bath-r-jose-2024-02-29', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-23', 5, 0, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.913366', '2026-09-04 10:41:06.913122');
INSERT INTO public.bathroom_summaries VALUES ('b9568f36-cf15-4431-afd7-9ba1663df428', 'seed-bath-r-jose-2024-03-01', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-24', 3, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.922138', '2026-09-04 10:41:06.921965');
INSERT INTO public.bathroom_summaries VALUES ('d858c844-8e4d-4673-9756-81e037d1d116', 'seed-bath-r-jose-2024-03-02', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-25', 3, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.929461', '2026-09-04 10:41:06.929283');
INSERT INTO public.bathroom_summaries VALUES ('fdf7755f-526f-4954-b356-97b468640e2b', 'seed-bath-r-jose-2024-03-03', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-26', 5, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.937351', '2026-09-04 10:41:06.937182');
INSERT INTO public.bathroom_summaries VALUES ('d6a519b2-4940-4b6d-ac8b-fb1263116edf', 'seed-bath-r-jose-2024-03-04', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-27', 5, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.944967', '2026-09-04 10:41:06.944771');
INSERT INTO public.bathroom_summaries VALUES ('25a20b8d-e730-46a5-8d39-a81863f43688', 'seed-bath-r-jose-2024-03-05', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-28', 4, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.952985', '2026-09-04 10:41:06.952741');
INSERT INTO public.bathroom_summaries VALUES ('3730e4d4-3d1b-48d8-8c87-5783ca378168', 'seed-bath-r-jose-2024-03-06', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-29', 2, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.960038', '2026-09-04 10:41:06.959866');
INSERT INTO public.bathroom_summaries VALUES ('33f0f9f2-21d0-42fd-8723-7bd339504e87', 'seed-bath-r-jose-2024-03-07', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-30', 4, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.967042', '2026-09-04 10:41:06.966885');
INSERT INTO public.bathroom_summaries VALUES ('ee1bd9a7-6a01-4c28-8ca9-d1a69a51b542', 'seed-bath-r-jose-2024-03-08', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-31', 6, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.974348', '2026-09-04 10:41:06.974123');
INSERT INTO public.bathroom_summaries VALUES ('b75277f8-3544-4189-944e-0899474190ec', 'seed-bath-r-jose-2024-03-09', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-01', 5, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.98248', '2026-09-04 10:41:06.982272');
INSERT INTO public.bathroom_summaries VALUES ('1fcb9a12-1b3a-40e8-9d35-c0748743d328', 'seed-bath-r-jose-2024-03-10', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-02', 3, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.990167', '2026-09-04 10:41:06.989962');
INSERT INTO public.bathroom_summaries VALUES ('40ae3d93-6676-4f79-b99c-14f5002a8400', 'seed-bath-r-jose-2024-03-11', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-03', 3, 2, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.997091', '2026-09-04 10:41:06.996923');


--
-- Data for Name: facilities; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.facilities VALUES ('9a8195a5-86b2-4492-93b0-35cbb362430e', 'Residencia Mana', 'America/Argentina/Buenos_Aires', NULL, NULL, '2026-09-04 10:41:06.550276', '2026-09-04 10:41:06.549352', 0);


--
-- Data for Name: wings; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.wings VALUES ('85eade2c-2ebe-4e80-b041-5484b395ffe1', '9a8195a5-86b2-4492-93b0-35cbb362430e', 'Ala Norte', '3', 1, NULL, NULL, '2026-09-04 10:41:06.561819', '2026-09-04 10:41:06.5611', 0);


--
-- Data for Name: rooms; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.rooms VALUES ('eb23d48e-1dcf-4f0e-83f4-37d7a278409f', '85eade2c-2ebe-4e80-b041-5484b395ffe1', '301', 'STANDARD', NULL, NULL, NULL, '2026-09-04 10:41:06.572517', '2026-09-04 10:41:06.571778', 0);


--
-- Data for Name: beds; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.beds VALUES ('819bfa2d-8002-45f8-976c-ea74caedf321', 'eb23d48e-1dcf-4f0e-83f4-37d7a278409f', 'Cama A', 'mon-r-jose', NULL, NULL, '2026-09-04 10:41:06.582245', '2026-09-04 10:41:06.581485', 0);


--
-- Data for Name: care_notes; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: care_summaries; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.care_summaries VALUES ('9aae1d66-a4f9-4a12-874b-e8b4fc027abc', 'seed-care-r-jose-2024-02-27', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-21', 35, 23, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.004673', '2026-09-04 10:41:07.00449', 0);
INSERT INTO public.care_summaries VALUES ('578dd0be-5bcb-4bca-abd5-5c580a22d090', 'seed-care-r-jose-2024-02-28', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-22', 27, 16, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.013749', '2026-09-04 10:41:07.013537', 0);
INSERT INTO public.care_summaries VALUES ('b6ccb383-7559-4432-b4bc-b6bc47178581', 'seed-care-r-jose-2024-02-29', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-23', 30, 12, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.021811', '2026-09-04 10:41:07.02162', 0);
INSERT INTO public.care_summaries VALUES ('472fc5ec-6b71-4276-a8cf-ecd4256d7c37', 'seed-care-r-jose-2024-03-01', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-24', 34, 22, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.030573', '2026-09-04 10:41:07.030358', 0);
INSERT INTO public.care_summaries VALUES ('0ed051fd-609e-4221-979c-361aa25c63ca', 'seed-care-r-jose-2024-03-02', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-25', 29, 20, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.038203', '2026-09-04 10:41:07.037964', 0);
INSERT INTO public.care_summaries VALUES ('17863841-8d98-4fd5-a633-92f4544338ae', 'seed-care-r-jose-2024-03-03', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-26', 28, 11, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.046437', '2026-09-04 10:41:07.046258', 0);
INSERT INTO public.care_summaries VALUES ('581113ed-67e8-4a7a-b67b-32c20aa7d104', 'seed-care-r-jose-2024-03-04', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-27', 31, 22, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.054719', '2026-09-04 10:41:07.054443', 0);
INSERT INTO public.care_summaries VALUES ('d815e479-7820-4bec-a302-7bc2b67333b5', 'seed-care-r-jose-2024-03-05', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-28', 46, 26, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.062351', '2026-09-04 10:41:07.062165', 0);
INSERT INTO public.care_summaries VALUES ('da333211-9de1-46d7-9188-28c127044a05', 'seed-care-r-jose-2024-03-06', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-29', 28, 20, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.070526', '2026-09-04 10:41:07.070281', 0);
INSERT INTO public.care_summaries VALUES ('5a496788-ac2c-40a7-b10e-1d0dfb05eb43', 'seed-care-r-jose-2024-03-07', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-30', 37, 22, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.07801', '2026-09-04 10:41:07.07784', 0);
INSERT INTO public.care_summaries VALUES ('42a170a2-b5fc-4006-88ca-75f3b41ea145', 'seed-care-r-jose-2024-03-08', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-31', 44, 20, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.085273', '2026-09-04 10:41:07.085093', 0);
INSERT INTO public.care_summaries VALUES ('ccfa1c4f-3232-4549-963d-639036e6ed65', 'seed-care-r-jose-2024-03-09', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-01', 42, 31, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.093271', '2026-09-04 10:41:07.093079', 0);
INSERT INTO public.care_summaries VALUES ('d8d620d2-14fd-4416-b819-78f72b759504', 'seed-care-r-jose-2024-03-10', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-02', 27, 10, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.10316', '2026-09-04 10:41:07.102949', 0);
INSERT INTO public.care_summaries VALUES ('ba5909e6-0619-4155-aa47-4eff22fab50c', 'seed-care-r-jose-2024-03-11', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-03', 41, 16, 0, 0, 'seed', NULL, NULL, '2026-09-04 10:41:07.110311', '2026-09-04 10:41:07.110066', 0);


--
-- Data for Name: clip_windows; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: current_bed_states; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.current_bed_states VALUES ('819bfa2d-8002-45f8-976c-ea74caedf321', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', 'sitting_in_bed', 'sitting_in_bed', NULL, false, '2026-09-04 10:41:19.131835', '2026-09-04 10:41:19.131835', 'STATE_CHANGE', 'seed-state-r-jose', false, '2026-09-04 10:41:19.131835');


--
-- Data for Name: episodes; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: episode_escalations; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: episode_notes; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: episode_timeline_events; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.episode_timeline_events VALUES ('jose-tl-01', '629e1bc1-b324-4674-bc93-e3e6c82cebe7', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-04 04:20:00', 'OPENED', 'laying_in_bed', 'sitting_in_bed', 'José se sentó en la cama.', '2026-09-04 10:42:03.94199');
INSERT INTO public.episode_timeline_events VALUES ('jose-tl-02', '629e1bc1-b324-4674-bc93-e3e6c82cebe7', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-04 04:35:00', 'UMBRELLA', 'sitting_in_bed', 'sitting_in_bed', 'Permaneció sentado durante 15 minutos.', '2026-09-04 10:42:03.94199');
INSERT INTO public.episode_timeline_events VALUES ('jose-tl-03', '629e1bc1-b324-4674-bc93-e3e6c82cebe7', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-04 04:38:00', 'RECOVERY', 'sitting_in_bed', 'laying_in_bed', 'José volvió a acostarse solo.', '2026-09-04 10:42:03.94199');


--
-- Data for Name: episode_transitions; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: evidence; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: facility_shifts; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: finding_policies; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.finding_policies VALUES ('7940e106-a7d5-4502-8b39-b134366abd68', NULL, true, '{"dawnTo": "06:05", "dawnFrom": "05:00", "dawnRatio": 0.66, "dawnMinCount": 3, "dropWoWEnabled": true, "dropWoWMinutes": 45, "exitsRisingFactor": 1.15, "dawnClusterEnabled": true, "exitsRisingEnabled": true, "exitsRisingMinDelta": 0.3, "restlessHighEnabled": true, "sleepInRangeEnabled": true, "restlessHighThreshold": 0.25, "sleepInRangeThreshold": 0.2, "restlessFragmentedEnabled": true, "restlessFragmentedThreshold": 0.35}', '{"careThinEnabled": true, "careThinMinutes": 20.0}', '{"nightMinAvg": 1.0, "nightRiseFactor": 1.5, "bathroomNightEnabled": true}', '2026-09-04 10:43:35.102485', 0);


--
-- Data for Name: history_episode_detections; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.history_episode_detections VALUES ('629e1bc1-b324-4674-bc93-e3e6c82cebe7', 'seed-ep-4', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '819bfa2d-8002-45f8-976c-ea74caedf321', NULL, 'BED_EXIT', 'warning', '2026-09-04 04:20:00', NULL, NULL, NULL, false, NULL, NULL, '[]', 'SENTINEL', NULL, NULL, '{}', '2026-09-04 10:41:06.611701', 0);
INSERT INTO public.history_episode_detections VALUES ('9143f025-5c64-404f-9264-364e57c3a416', 'seed-ep-jose-fall-history', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '819bfa2d-8002-45f8-976c-ea74caedf321', NULL, 'FALL', 'critical', '2026-08-26 22:40:00', NULL, NULL, NULL, false, NULL, NULL, '[]', 'SENTINEL', NULL, NULL, '{}', '2026-09-04 10:41:06.655519', 0);


--
-- Data for Name: staff_members; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: history_episode_interventions; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: history_episode_reviews; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.history_episode_reviews VALUES ('6de7cc5b-5298-434a-a1a5-10f96746c122', '629e1bc1-b324-4674-bc93-e3e6c82cebe7', 'resolved', 'confirmed', 'Revisión de demo', '2026-09-04 10:44:03.738473', 'gaston', '2026-09-04 10:44:03.739032', 0, '2026-09-04 10:44:03.738501');


--
-- Data for Name: hub_policy_outbox; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: mobility_summaries; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.mobility_summaries VALUES ('d4147987-fa02-4a34-ac45-2ab0e5e7767d', 'seed-mob-r-jose-2024-02-27', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-21', 0, 220, 0, 0, 0, 9, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.782823', '2026-09-04 10:41:06.782628');
INSERT INTO public.mobility_summaries VALUES ('85c672a4-2dbb-48de-8c8b-31c1cd9e60d4', 'seed-mob-r-jose-2024-02-28', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-22', 0, 181, 0, 0, 0, 4, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.790764', '2026-09-04 10:41:06.790516');
INSERT INTO public.mobility_summaries VALUES ('aa8dd845-9745-45c7-a49b-8256edfe82b1', 'seed-mob-r-jose-2024-02-29', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-23', 0, 231, 0, 0, 0, 9, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.798661', '2026-09-04 10:41:06.79847');
INSERT INTO public.mobility_summaries VALUES ('ea0429f7-99b3-4c67-a85f-bafa90f64544', 'seed-mob-r-jose-2024-03-01', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-24', 0, 246, 0, 0, 0, 6, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.806749', '2026-09-04 10:41:06.80655');
INSERT INTO public.mobility_summaries VALUES ('6d510966-c7d4-4e50-a01e-54bc3612dcac', 'seed-mob-r-jose-2024-03-02', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-25', 0, 245, 0, 0, 0, 6, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.814934', '2026-09-04 10:41:06.814508');
INSERT INTO public.mobility_summaries VALUES ('4bba7bea-0a10-42e0-b1b9-2b231564ca9c', 'seed-mob-r-jose-2024-03-03', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-26', 0, 190, 0, 0, 0, 5, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.823445', '2026-09-04 10:41:06.82323');
INSERT INTO public.mobility_summaries VALUES ('72812f80-01dc-4cd0-b498-54331a5c225f', 'seed-mob-r-jose-2024-03-04', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-27', 0, 222, 0, 0, 0, 7, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.832676', '2026-09-04 10:41:06.832405');
INSERT INTO public.mobility_summaries VALUES ('9bbc7534-0266-4a99-ad50-aa8f81ed77a4', 'seed-mob-r-jose-2024-03-05', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-28', 0, 214, 0, 0, 0, 4, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.839849', '2026-09-04 10:41:06.839649');
INSERT INTO public.mobility_summaries VALUES ('4fd8ec75-91dc-417b-a180-75aaf126a2fc', 'seed-mob-r-jose-2024-03-06', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-29', 0, 224, 0, 0, 0, 6, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.847428', '2026-09-04 10:41:06.847126');
INSERT INTO public.mobility_summaries VALUES ('caf70daa-555c-445e-9782-6c432687c42e', 'seed-mob-r-jose-2024-03-07', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-30', 0, 215, 0, 0, 0, 4, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.855686', '2026-09-04 10:41:06.855422');
INSERT INTO public.mobility_summaries VALUES ('32fda201-e347-4b82-bca7-e15dbb295700', 'seed-mob-r-jose-2024-03-08', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-31', 0, 194, 0, 0, 0, 8, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.863447', '2026-09-04 10:41:06.863244');
INSERT INTO public.mobility_summaries VALUES ('3c16b4ac-7056-4a79-b55f-22117b58a686', 'seed-mob-r-jose-2024-03-09', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-01', 0, 247, 0, 0, 0, 8, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.871515', '2026-09-04 10:41:06.871316');
INSERT INTO public.mobility_summaries VALUES ('20ee53dc-747b-4dfc-816f-cefc6b1ac9d2', 'seed-mob-r-jose-2024-03-10', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-02', 0, 215, 0, 0, 0, 5, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.879029', '2026-09-04 10:41:06.878791');
INSERT INTO public.mobility_summaries VALUES ('1c99dd17-3260-47a3-9d4d-75724c867639', 'seed-mob-r-jose-2024-03-11', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-03', 0, 205, 0, 0, 0, 5, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.887075', '2026-09-04 10:41:06.886772');


--
-- Data for Name: notification_deliveries; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: notification_delivery_events; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: notification_events; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: planogram_placements; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: policy_recommendations; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: residents; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.residents VALUES ('dff8ece6-2d6a-4edb-a5bf-883a007a1b67', 'r-jose', 'José Ferreyra', '1941-06-18', '2023-09-04', 'active', NULL, NULL, '2026-09-04 10:41:06.591376', '2026-09-04 10:41:06.590987', 0);


--
-- Data for Name: resident_bed_assignments; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.resident_bed_assignments VALUES ('3bac7e4e-a938-4bc2-8741-1edfea9efa9c', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '819bfa2d-8002-45f8-976c-ea74caedf321', '2026-09-04 10:41:06.601721', NULL, '2026-09-04 10:41:06.601985', NULL, 0);


--
-- Data for Name: resident_notes; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: resident_profiles; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: room_privacy_regions; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: rounds; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: round_tasks; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: scene_events; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.scene_events VALUES ('38a71a17-d1b5-4132-82f7-0047c0799a4d', 'seed-ep-4-sc-10', '819bfa2d-8002-45f8-976c-ea74caedf321', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', 'STATE_CHANGED', 'UNKNOWN', 'SITTING_IN_BED', NULL, '2026-09-04 04:20:00', '{"id":"sc-10","at":"2024-03-12T04:20:00.000Z","fromState":"laying_in_bed","toState":"sitting_in_bed","label":"Se sentó en la cama"}', '2026-09-04 10:41:06.626574', '{}', NULL, NULL, NULL, NULL, '2026-09-04 10:41:06.627855');
INSERT INTO public.scene_events VALUES ('4f71fc9f-c5de-47a8-a46a-b42b84295f37', 'seed-ep-4-sc-11', '819bfa2d-8002-45f8-976c-ea74caedf321', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', 'STATE_CHANGED', 'SITTING_IN_BED', 'SITTING_IN_BED', NULL, '2026-09-04 04:35:00', '{"id":"sc-11","at":"2024-03-12T04:35:00.000Z","fromState":"sitting_in_bed","toState":"sitting_in_bed","label":"Permanencia sentado: 15 min"}', '2026-09-04 10:41:06.636498', '{}', NULL, NULL, NULL, NULL, '2026-09-04 10:41:06.637586');
INSERT INTO public.scene_events VALUES ('c17a3944-8de5-4110-babb-dabf96953ede', 'seed-ep-4-sc-12', '819bfa2d-8002-45f8-976c-ea74caedf321', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', 'STATE_CHANGED', 'SITTING_IN_BED', 'UNKNOWN', NULL, '2026-09-04 04:38:00', '{"id":"sc-12","at":"2024-03-12T04:38:00.000Z","fromState":"sitting_in_bed","toState":"laying_in_bed","label":"Volvió a acostarse"}', '2026-09-04 10:41:06.646346', '{}', NULL, NULL, NULL, NULL, '2026-09-04 10:41:06.647628');


--
-- Data for Name: sensor_events; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: sentinel_signals; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: shift_notes; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: sleep_summaries; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.sleep_summaries VALUES ('c456a6e6-2046-4d39-b814-c5f3b442c946', 'seed-sleep-r-jose-2024-02-27', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-21', 358, 65, 40, 0, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.665857', '2026-09-04 10:41:06.665647', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('9fa0c3e9-11bb-4736-a4d2-15c7b660db34', 'seed-sleep-r-jose-2024-02-28', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-22', 377, 55, 27, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.674389', '2026-09-04 10:41:06.674124', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('ea76eead-87b1-4747-b1d2-5eb186154c7e', 'seed-sleep-r-jose-2024-02-29', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-23', 353, 56, 59, 0, 0, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.682847', '2026-09-04 10:41:06.682663', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('cd268c53-4dbb-4a11-b4e4-e78970d117cc', 'seed-sleep-r-jose-2024-03-01', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-24', 321, 41, 32, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.690421', '2026-09-04 10:41:06.690137', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('4a400de3-4b21-47b9-a9ba-f06f989bc1c5', 'seed-sleep-r-jose-2024-03-02', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-25', 338, 62, 36, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.698299', '2026-09-04 10:41:06.698122', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('a0e874da-02c9-4a59-a2e3-e8937b4e2094', 'seed-sleep-r-jose-2024-03-03', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-26', 360, 48, 40, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.708166', '2026-09-04 10:41:06.707913', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('c8bbc7b9-a9cc-4134-81c8-8fc2e7d1518b', 'seed-sleep-r-jose-2024-03-04', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-27', 323, 49, 49, 0, 2, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.716357', '2026-09-04 10:41:06.716144', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('1b87b973-d9aa-4f2c-97c9-578d0497c6a8', 'seed-sleep-r-jose-2024-03-05', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-28', 359, 53, 33, 0, 2, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.72724', '2026-09-04 10:41:06.727019', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('73799115-a611-4198-988e-e08d0f9e7e92', 'seed-sleep-r-jose-2024-03-06', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-29', 322, 54, 28, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.734984', '2026-09-04 10:41:06.73475', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('6fd65936-f9b5-4d82-b68e-5dcc7f266e29', 'seed-sleep-r-jose-2024-03-07', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-30', 376, 51, 46, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.742932', '2026-09-04 10:41:06.742597', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('5b7d6cc5-ce4a-4248-adce-edc27ed8919b', 'seed-sleep-r-jose-2024-03-08', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-08-31', 360, 42, 44, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.750478', '2026-09-04 10:41:06.750305', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('415e6b57-a76a-4f4c-9ea9-8ead50cdf5a4', 'seed-sleep-r-jose-2024-03-09', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-01', 308, 75, 37, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.758457', '2026-09-04 10:41:06.758266', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('cbf2e956-124e-4468-b962-9be9a57eb0d5', 'seed-sleep-r-jose-2024-03-10', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-02', 342, 72, 52, 0, 2, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.766158', '2026-09-04 10:41:06.765979', NULL, NULL);
INSERT INTO public.sleep_summaries VALUES ('4f47bade-c2c6-4a69-a7b8-903ce8381268', 'seed-sleep-r-jose-2024-03-11', 'dff8ece6-2d6a-4edb-a5bf-883a007a1b67', '2026-09-03', 350, 50, 49, 0, 1, 0, 'seed', NULL, NULL, '{}', '2026-09-04 10:41:06.774587', '2026-09-04 10:41:06.774412', NULL, NULL);


--
-- Data for Name: staff_groups; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: streams; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: stream_regions; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: timelines; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: unit_shift_coverages; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- PostgreSQL database dump complete
--

\unrestrict utVrHJx1h7g9V21IXC41OTgjSXgdnOil8GYwiYkMUMQHvRN6wNdf3R9LZRidMbd

