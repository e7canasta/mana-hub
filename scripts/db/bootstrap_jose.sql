-- Bootstrap demo: José (historial) + Susan (alta reciente). Summaries vía seed API.

INSERT INTO facilities (id, name, timezone, created_at, updated_at, version)
VALUES ('fac-001', 'Residencia Los Robles', 'America/Argentina/Buenos_Aires', NOW(), NOW(), 0);

INSERT INTO wings (id, facility_id, name, floor, sort_order, created_at, updated_at, version)
VALUES ('wing-a', 'fac-001', 'Ala A - Piso Bajo', 'Bajo', 0, NOW(), NOW(), 0);

INSERT INTO rooms (id, wing_id, number, created_at, updated_at, version)
VALUES ('room-101', 'wing-a', '101', NOW(), NOW(), 0);

INSERT INTO rooms (id, wing_id, number, created_at, updated_at, version)
VALUES ('room-102', 'wing-a', '102', NOW(), NOW(), 0);

INSERT INTO beds (id, room_id, label, monitor_key, created_at, updated_at, version)
VALUES ('bed-1', 'room-101', 'Cama 1', 'cam-001', NOW(), NOW(), 0);

INSERT INTO beds (id, room_id, label, monitor_key, created_at, updated_at, version)
VALUES ('bed-2', 'room-102', 'Cama 1', 'cam-002', NOW(), NOW(), 0);

INSERT INTO residents (
    id, external_id, full_name, birth_date, admission_date, status, created_at, updated_at, version
) VALUES (
    'jose', 'jose', 'José García', '1942-03-15', '2024-01-15', 'active', NOW(), NOW(), 0
);

INSERT INTO residents (
    id, external_id, full_name, birth_date, admission_date, status, created_at, updated_at, version
) VALUES (
    'susan', 'susan', 'Susan Martínez', '1955-07-22', CURRENT_DATE - INTERVAL '1 day', 'active', NOW(), NOW(), 0
);

INSERT INTO resident_bed_assignments (id, resident_id, bed_id, starts_at, created_at, version)
VALUES ('assign-jose', 'jose', 'bed-1', NOW(), NOW(), 0);

INSERT INTO resident_bed_assignments (id, resident_id, bed_id, starts_at, created_at, version)
VALUES ('assign-susan', 'susan', 'bed-2', NOW(), NOW(), 0);

INSERT INTO current_bed_states (bed_id, resident_id, state, staff_present, state_since, updated_at)
VALUES ('bed-1', 'jose', 'lying', FALSE, NOW(), NOW());

INSERT INTO current_bed_states (bed_id, resident_id, state, staff_present, state_since, updated_at)
VALUES ('bed-2', 'susan', 'lying', FALSE, NOW(), NOW());
