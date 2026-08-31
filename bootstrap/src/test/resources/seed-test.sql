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

INSERT INTO alarm_profile_versions (id, resident_id, valid_from, risk_level, mobility_aid, autopilot, mode, template_id, updated_by, created_at, version)
VALUES ('profile-jose-v1', 'jose', now(), 'LOW', 'NONE', false, 'PRESET', 'standard', 'seed', now(), 0)
ON CONFLICT DO NOTHING;
