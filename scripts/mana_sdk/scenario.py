"""Aplicar escenarios declarativos (demo_panel.py) sobre ResidentChart."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone

from .resident import Resident


def _days_ago(days: int) -> datetime:
    return datetime.now(timezone.utc) - timedelta(days=days)


def _iso(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")


def apply_resident_spec(
    resident: Resident,
    spec: dict,
    *,
    wellbeing_days: int = 14,
) -> dict[str, int]:
    """
    Carga perfil E1 + wellbeing + historia desde un dict estilo demo_panel.
    Retorna contadores de lo aplicado.
    """
    stats = {"alarms": 0, "wellbeing_days": 0, "history": 0, "state": 0}

    if alarm := spec.get("alarm"):
        resident.alarms.apply(alarm)
        resident.alarms.push()
        stats["alarms"] = 1

    profile = spec.get("wellbeing", "stable")
    stats["wellbeing_days"] = resident.seed_wellbeing_profile(profile, days=wellbeing_days)

    if state := spec.get("state"):
        resident.set_live_state(state, ignore_errors=True)
        stats["state"] = 1

    for fall in spec.get("falls", []):
        resident.ingest_history(
            source_record_id=f"seed-{spec['key']}-fall-{fall['days_ago']}",
            kind="FALL",
            severity="CRITICAL",
            occurred_at=_days_ago(fall["days_ago"]),
            narrative=fall.get("narrative"),
        )
        stats["history"] += 1

    mon = spec.get("monitoring_since")
    if mon:
        resident.ingest_history(
            source_record_id=f"seed-{spec['key']}-monitoring",
            kind=mon.get("kind", "OTHER"),
            severity="INFO",
            occurred_at=_days_ago(mon["days_ago"]),
            narrative=mon.get("narrative"),
        )
        stats["history"] += 1

    resident.refresh(wellbeing_days=wellbeing_days)
    return stats
