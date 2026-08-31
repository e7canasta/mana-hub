"""
Escenario demo panel: José (historial E1) + Susan (alta reciente, sin baseline).

José — perfil E1 (jose-e1-full.json), 14 días de summaries.
Susan — admitida ayer, 1 día de observación, sin caídas ni cuidado sembrado.

  python scripts/seed/run.py
  python scripts/seed/run.py --residents jose,susan --verify
"""

from __future__ import annotations

FACILITY = {
    "name": "Residencia Los Robles",
    "timezone": "America/Argentina/Buenos_Aires",
    "wings": [
        {
            "name": "Ala A - Piso Bajo",
            "floor": "Bajo",
            "rooms": [
                {"number": "101", "beds": [{"label": "Cama 1", "monitor_key": "cam-001"}]},
                {"number": "102", "beds": [{"label": "Cama 1", "monitor_key": "cam-002"}]},
            ],
        },
    ],
}

ADMISSION_DATE = "2024-01-15"
WELLBEING_DAYS = 14

# Perfil E1 → alarm-presets (ComeBack en acostado)
Jose_E1_ALARM = {
    "risk_level": "high",
    "mobility_aid": "walker",
    "autopilot": False,
    "mode": "custom",
    "template_id": "standard",
    "reason": "Configuración E1: ComeBack 12/15m + reglas de grabación",
    "overrides": {
        "lying": {
            "type": "comeback",
            "baselineState": "lying",
            "warningAfterMinutes": 12,
            "alertAfterMinutes": 15,
            "severity": "WARNING",
            "closureCondition": "STAFF_OR_SAFE",
        },
        "lying_to_sitting": {
            "type": "hysteresis",
            "transitionKey": "lying_to_sitting",
            "hysteresisSeconds": 2,
        },
    },
}

SUSAN_STANDARD_ALARM = {
    "risk_level": "medium",
    "mobility_aid": "none",
    "autopilot": True,
    "mode": "template",
    "template_id": "standard",
    "reason": "Alta reciente — perfil estándar hasta baseline",
    "overrides": {},
}

RESIDENTS = [
    {
        "key": "jose",
        "full_name": "José García",
        "birth_date": "1942-03-15",
        "admission_date": ADMISSION_DATE,
        "wing": "Ala A - Piso Bajo",
        "room": "101",
        "bed_label": "Cama 1",
        "state": "lying",
        "staff_present": False,
        "wellbeing": "restless",
        "wellbeing_days": WELLBEING_DAYS,
        "alarm": Jose_E1_ALARM,
        "falls": [
            {
                "days_ago": 2,
                "injury": "minor",
                "narrative": "Caída en baño. Consciente, corte superficial rodilla.",
            },
            {
                "days_ago": 45,
                "injury": "none",
                "narrative": "Caída leve al levantarse. Recuperación autónoma.",
            },
        ],
        "monitoring_since": {
            "days_ago": 230,
            "kind": "OTHER",
            "narrative": "Monitoreo E1 desde admisión (2024-01-15).",
        },
    },
    {
        "key": "susan",
        "full_name": "Susan Martínez",
        "birth_date": "1955-07-22",
        "admission_days_ago": 1,
        "wing": "Ala A - Piso Bajo",
        "room": "102",
        "bed_label": "Cama 1",
        "state": "lying",
        "staff_present": False,
        "wellbeing": "stable",
        "wellbeing_days": 1,
        "skip_care": True,
        "alarm": SUSAN_STANDARD_ALARM,
        "falls": [],
    },
]

RESIDENTS_UNASSIGNED: list[dict] = []
