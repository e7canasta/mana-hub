-- =============================================
-- seed-test.sql — insights integration
-- Datos mínimos para los repos que FindingService consulta:
--   ResidentRepository, SummaryRepository, SceneEventRepository,
--   CareSummaryRepository, HistoryEpisodeDetectionRepository,
--   AlarmProfileRepository, AlarmProfileOverrideRepository
-- =============================================

-- ctx-residencia: facility → wing → room → bed
INSERT INTO facilities (id, name, timezone, created_at, updated_at, version)
VALUES ('fac-001', 'Residencia Los Robles', 'America/Argentina/Buenos_Aires', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO wings (id, facility_id, name, floor, sort_order, created_at, updated_at, version)
VALUES ('wing-a', 'fac-001', 'Ala Norte', 'Bajo', 0, now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO rooms (id, wing_id, number, created_at, updated_at, version)
VALUES ('room-301', 'wing-a', '301', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO rooms (id, wing_id, number, created_at, updated_at, version)
VALUES ('room-302', 'wing-a', '302', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO beds (id, room_id, label, monitor_key, created_at, updated_at, version)
VALUES ('bed-4', 'room-301', 'Cama A', 'm1', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO beds (id, room_id, label, monitor_key, created_at, updated_at, version)
VALUES ('bed-5', 'room-302', 'Cama B', 'm2', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

-- ctx-poblacion: residentes + asignaciones
-- jose: admisión 2024-01-15, duerme bien, movilidad normal
-- maria: admisión 2024-02-01, sueño inquieto, movilidad limitada
INSERT INTO residents (id, full_name, birth_date, admission_date, status, created_at, updated_at, version)
VALUES ('jose', 'José García', '1942-03-15'::date, '2024-01-15'::date, 'active', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO residents (id, full_name, birth_date, admission_date, status, created_at, updated_at, version)
VALUES ('maria', 'María García', '1940-03-15'::date, '2024-02-01'::date, 'active', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO resident_bed_assignments (id, resident_id, bed_id, starts_at, created_at, version)
VALUES ('assign-jose', 'jose', 'bed-4', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO resident_bed_assignments (id, resident_id, bed_id, starts_at, created_at, version)
VALUES ('assign-maria', 'maria', 'bed-5', now(), now(), 0)
ON CONFLICT (id) DO NOTHING;

-- ctx-politica: alarm profiles + overrides
INSERT INTO alarm_profile_versions (id, resident_id, valid_from, risk_level, mobility_aid, autopilot, mode, template_id, updated_by, created_at, version)
VALUES ('profile-jose-v1', 'jose', now(), 'LOW', 'NONE', false, 'PRESET', 'standard', 'seed', now(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO alarm_profile_versions (id, resident_id, valid_from, risk_level, mobility_aid, autopilot, mode, template_id, updated_by, created_at, version)
VALUES ('profile-maria-v1', 'maria', now(), 'MEDIUM', 'NONE', false, 'PRESET', 'standard', 'seed', now(), 0)
ON CONFLICT DO NOTHING;

INSERT INTO alarm_profile_overrides (id, profile_version_id, rule_id, override_type, state_kind, warning_after_minutes, alert_after_minutes, created_at)
VALUES ('override-jose-bed', 'profile-jose-v1', 'BED_EXIT', 'dwell', 'OUT_OF_BED', 5, 15, now())
ON CONFLICT DO NOTHING;

-- mana-observation: sleep_summaries (7 días para baseline)
-- jose: sueño estable (~360 calm, ~30 restless)
INSERT INTO sleep_summaries (id, source_record_id, resident_id, observed_on, calm_minutes, restless_minutes, awake_minutes, out_of_bed_minutes, bed_exit_count, wake_count, source, created_at, updated_at)
VALUES
  ('ss-j-20260825', 'src-j-20260825', 'jose', '2026-08-25', 360, 30, 20, 10, 1, 1, 'seed', now(), now()),
  ('ss-j-20260826', 'src-j-20260826', 'jose', '2026-08-26', 370, 25, 15, 5, 0, 1, 'seed', now(), now()),
  ('ss-j-20260827', 'src-j-20260827', 'jose', '2026-08-27', 350, 40, 25, 15, 2, 2, 'seed', now(), now()),
  ('ss-j-20260828', 'src-j-20260828', 'jose', '2026-08-28', 380, 20, 10, 5, 0, 1, 'seed', now(), now()),
  ('ss-j-20260829', 'src-j-20260829', 'jose', '2026-08-29', 365, 35, 18, 8, 1, 1, 'seed', now(), now()),
  ('ss-j-20260830', 'src-j-20260830', 'jose', '2026-08-30', 375, 22, 12, 6, 0, 1, 'seed', now(), now()),
  ('ss-j-20260831', 'src-j-20260831', 'jose', '2026-08-31', 360, 30, 20, 10, 1, 1, 'seed', now(), now())
ON CONFLICT (id) DO NOTHING;

-- maria: sueño inquieto (~275 calm, ~85 restless, ~65 awake)
INSERT INTO sleep_summaries (id, source_record_id, resident_id, observed_on, calm_minutes, restless_minutes, awake_minutes, out_of_bed_minutes, bed_exit_count, wake_count, source, created_at, updated_at)
VALUES
  ('ss-m-20260825', 'src-m-20260825', 'maria', '2026-08-25', 280, 80, 60, 40, 4, 5, 'seed', now(), now()),
  ('ss-m-20260826', 'src-m-20260826', 'maria', '2026-08-26', 260, 90, 70, 50, 5, 6, 'seed', now(), now()),
  ('ss-m-20260827', 'src-m-20260827', 'maria', '2026-08-27', 290, 75, 55, 35, 3, 4, 'seed', now(), now()),
  ('ss-m-20260828', 'src-m-20260828', 'maria', '2026-08-28', 270, 85, 65, 45, 4, 5, 'seed', now(), now()),
  ('ss-m-20260829', 'src-m-20260829', 'maria', '2026-08-29', 255, 95, 75, 55, 6, 7, 'seed', now(), now()),
  ('ss-m-20260830', 'src-m-20260830', 'maria', '2026-08-30', 285, 78, 58, 38, 3, 4, 'seed', now(), now()),
  ('ss-m-20260831', 'src-m-20260831', 'maria', '2026-08-31', 275, 82, 62, 42, 4, 5, 'seed', now(), now())
ON CONFLICT (id) DO NOTHING;

-- mana-observation: bathroom_summaries
-- jose: normal (2-3 visitas, 0-1 nocturnas)
INSERT INTO bathroom_summaries (id, source_record_id, resident_id, observed_on, visit_count, night_visit_count, assisted_count, total_minutes, source, created_at, updated_at)
VALUES
  ('bs-j-20260825', 'bsrc-j-20260825', 'jose', '2026-08-25', 3, 1, 0, 15, 'seed', now(), now()),
  ('bs-j-20260826', 'bsrc-j-20260826', 'jose', '2026-08-26', 2, 0, 0, 10, 'seed', now(), now()),
  ('bs-j-20260827', 'bsrc-j-20260827', 'jose', '2026-08-27', 3, 1, 0, 15, 'seed', now(), now()),
  ('bs-j-20260828', 'bsrc-j-20260828', 'jose', '2026-08-28', 2, 0, 0, 10, 'seed', now(), now()),
  ('bs-j-20260829', 'bsrc-j-20260829', 'jose', '2026-08-29', 3, 1, 0, 15, 'seed', now(), now()),
  ('bs-j-20260830', 'bsrc-j-20260830', 'jose', '2026-08-30', 2, 0, 0, 10, 'seed', now(), now()),
  ('bs-j-20260831', 'bsrc-j-20260831', 'jose', '2026-08-31', 3, 1, 0, 15, 'seed', now(), now())
ON CONFLICT (id) DO NOTHING;

-- maria: nocturnas frecuentes (3-5 nocturnas, asistida)
INSERT INTO bathroom_summaries (id, source_record_id, resident_id, observed_on, visit_count, night_visit_count, assisted_count, total_minutes, source, created_at, updated_at)
VALUES
  ('bs-m-20260825', 'bsrc-m-20260825', 'maria', '2026-08-25', 5, 3, 1, 30, 'seed', now(), now()),
  ('bs-m-20260826', 'bsrc-m-20260826', 'maria', '2026-08-26', 6, 4, 2, 35, 'seed', now(), now()),
  ('bs-m-20260827', 'bsrc-m-20260827', 'maria', '2026-08-27', 4, 2, 1, 25, 'seed', now(), now()),
  ('bs-m-20260828', 'bsrc-m-20260828', 'maria', '2026-08-28', 5, 3, 1, 30, 'seed', now(), now()),
  ('bs-m-20260829', 'bsrc-m-20260829', 'maria', '2026-08-29', 7, 5, 2, 40, 'seed', now(), now()),
  ('bs-m-20260830', 'bsrc-m-20260830', 'maria', '2026-08-30', 4, 2, 1, 25, 'seed', now(), now()),
  ('bs-m-20260831', 'bsrc-m-20260831', 'maria', '2026-08-31', 5, 3, 1, 30, 'seed', now(), now())
ON CONFLICT (id) DO NOTHING;

-- mana-observation: mobility_summaries
-- jose: camina bien (~60 min/día, ~300m)
INSERT INTO mobility_summaries (id, source_record_id, resident_id, observed_on, in_bed_minutes, out_of_bed_minutes, out_of_sight_minutes, walking_minutes, distance_meters, transfer_count, source, created_at, updated_at)
VALUES
  ('ms-j-20260825', 'msrc-j-20260825', 'jose', '2026-08-25', 480, 240, 20, 60, 300.0, 8, 'seed', now(), now()),
  ('ms-j-20260826', 'msrc-j-20260826', 'jose', '2026-08-26', 490, 230, 15, 55, 275.0, 7, 'seed', now(), now()),
  ('ms-j-20260827', 'msrc-j-20260827', 'jose', '2026-08-27', 475, 245, 25, 65, 325.0, 9, 'seed', now(), now()),
  ('ms-j-20260828', 'msrc-j-20260828', 'jose', '2026-08-28', 485, 235, 18, 58, 290.0, 8, 'seed', now(), now()),
  ('ms-j-20260829', 'msrc-j-20260829', 'jose', '2026-08-29', 480, 240, 20, 60, 300.0, 8, 'seed', now(), now()),
  ('ms-j-20260830', 'msrc-j-20260830', 'jose', '2026-08-30', 490, 230, 15, 55, 275.0, 7, 'seed', now(), now()),
  ('ms-j-20260831', 'msrc-j-20260831', 'jose', '2026-08-31', 480, 240, 20, 60, 300.0, 8, 'seed', now(), now())
ON CONFLICT (id) DO NOTHING;

-- maria: movilidad limitada (~17 min/día caminando, ~85m)
INSERT INTO mobility_summaries (id, source_record_id, resident_id, observed_on, in_bed_minutes, out_of_bed_minutes, out_of_sight_minutes, walking_minutes, distance_meters, transfer_count, source, created_at, updated_at)
VALUES
  ('ms-m-20260825', 'msrc-m-20260825', 'maria', '2026-08-25', 540, 180, 40, 20, 100.0, 4, 'seed', now(), now()),
  ('ms-m-20260826', 'msrc-m-20260826', 'maria', '2026-08-26', 550, 170, 45, 15, 75.0, 3, 'seed', now(), now()),
  ('ms-m-20260827', 'msrc-m-20260827', 'maria', '2026-08-27', 535, 185, 35, 25, 125.0, 5, 'seed', now(), now()),
  ('ms-m-20260828', 'msrc-m-20260828', 'maria', '2026-08-28', 545, 175, 42, 18, 90.0, 4, 'seed', now(), now()),
  ('ms-m-20260829', 'msrc-m-20260829', 'maria', '2026-08-29', 555, 165, 50, 10, 50.0, 3, 'seed', now(), now()),
  ('ms-m-20260830', 'msrc-m-20260830', 'maria', '2026-08-30', 540, 180, 40, 20, 100.0, 4, 'seed', now(), now()),
  ('ms-m-20260831', 'msrc-m-20260831', 'maria', '2026-08-31', 548, 172, 43, 17, 85.0, 3, 'seed', now(), now())
ON CONFLICT (id) DO NOTHING;

-- mana-observation: scene_events (jose se levanta de la cama, maria frecuentes salidas)
INSERT INTO scene_events (id, event_id, bed_id, resident_id, event_type, from_state, to_state, trigger_type, timestamp, payload_json, received_at)
VALUES
  ('se-j-001', 'evt-j-001', 'bed-4', 'jose', 'TRANSITION', 'IN_BED', 'OUT_OF_BED', 'MOTION', '2026-08-31 02:30:00', '{}', now()),
  ('se-j-002', 'evt-j-002', 'bed-4', 'jose', 'TRANSITION', 'OUT_OF_BED', 'IN_BED', 'MOTION', '2026-08-31 02:45:00', '{}', now()),
  ('se-j-003', 'evt-j-003', 'bed-4', 'jose', 'TRANSITION', 'IN_BED', 'OUT_OF_BED', 'MOTION', '2026-08-31 05:00:00', '{}', now()),
  ('se-m-001', 'evt-m-001', 'bed-5', 'maria', 'TRANSITION', 'IN_BED', 'OUT_OF_BED', 'MOTION', '2026-08-31 01:15:00', '{}', now()),
  ('se-m-002', 'evt-m-002', 'bed-5', 'maria', 'TRANSITION', 'OUT_OF_BED', 'IN_BED', 'MOTION', '2026-08-31 01:40:00', '{}', now()),
  ('se-m-003', 'evt-m-003', 'bed-5', 'maria', 'TRANSITION', 'IN_BED', 'OUT_OF_BED', 'MOTION', '2026-08-31 03:00:00', '{}', now()),
  ('se-m-004', 'evt-m-004', 'bed-5', 'maria', 'TRANSITION', 'OUT_OF_BED', 'IN_BED', 'MOTION', '2026-08-31 03:20:00', '{}', now()),
  ('se-m-005', 'evt-m-005', 'bed-5', 'maria', 'TRANSITION', 'IN_BED', 'OUT_OF_BED', 'MOTION', '2026-08-31 05:30:00', '{}', now())
ON CONFLICT (event_id) DO NOTHING;
