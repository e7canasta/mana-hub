"""
Resident — raíz del DSL navegable.

  hub = ManaHub()
  jose = hub.residents.get("jose")

  jose.chart.location          # dónde estoy parado en identidad/ubicación
  jose.wellbeing.sleep         # dimensión sueño (14 días)
  jose.wellbeing.sleep["2026-08-30"].calm_minutes = 400
  jose.alarms.risk_level = "high"
  jose.falls.streak_days
  jose.episodes.falls()
  jose.push()
"""

from __future__ import annotations

import uuid
from datetime import date, datetime, timezone
from typing import Any

from .client import ManaClient, ManaError
from .models import BathroomDay, MobilityDay, SleepDay
from . import wellbeing
from .views import (
    AlarmsView,
    ChartView,
    EpisodesView,
    FallsView,
    WellbeingView,
)


class Resident:
    def __init__(self, client: ManaClient, resident_id: str, *, key: str | None = None):
        self._client = client
        self.id = resident_id
        self.key = key or resident_id
        self._path = f"mana.residents[{self.key!r}]"
        self._dirty_wellbeing: set[str] = set()

        self.chart = ChartView(self)
        self.wellbeing = WellbeingView(self)
        self.alarms = AlarmsView(self)
        self.falls = FallsView(self)
        self.episodes = EpisodesView(self)

    # ── compat aliases (deprecated paths) ─────────────────────────

    @property
    def alarm(self) -> AlarmsView:
        return self.alarms

    @property
    def sleep(self):
        return self.wellbeing.sleep

    @property
    def mobility(self):
        return self.wellbeing.mobility

    @property
    def bathroom(self):
        return self.wellbeing.bathroom

    @property
    def full_name(self) -> str:
        return self.chart.full_name

    @property
    def location(self):
        return self.chart.location

    @property
    def current_state(self):
        return self.chart.live_state

    # ── carga ─────────────────────────────────────────────────────

    def refresh(self, *, wellbeing_days: int = 14) -> Resident:
        """Hidrata todo el chart (como useResidentChart en el panel)."""
        self.chart.refresh()
        self.alarms.refresh()
        self.falls.refresh()
        self.episodes.refresh()
        self.wellbeing.load(days=wellbeing_days)
        return self

    # ── persistencia ──────────────────────────────────────────────

    def push(self) -> dict[str, int]:
        """Guarda todo lo editado (alarms + wellbeing dirty)."""
        result = self.wellbeing.push()
        if self.alarms._data._dirty:
            self.alarms.push()
            result["alarms"] = 1
        else:
            result["alarms"] = 0
        return result

    def save_alarms(self) -> AlarmsView:
        return self.alarms.push()

    # ── seeds / helpers ───────────────────────────────────────────

    def seed_wellbeing_profile(self, profile: str, *, days: int = 14) -> int:
        count = 0
        for day in wellbeing.date_range(days):
            payload = wellbeing.generate_day(profile, self.key, day)
            self._ingest_day_raw("sleep", day, payload["sleep"])
            self._ingest_day_raw("mobility", day, payload["mobility"])
            self._ingest_day_raw("bathroom", day, payload["bathroom"])
            count += 1
        self.refresh(wellbeing_days=days)
        return count

    def ingest_history(
        self,
        *,
        source_record_id: str,
        kind: str,
        severity: str = "INFO",
        occurred_at: datetime | None = None,
        narrative: str | None = None,
        ignore_duplicates: bool = True,
    ) -> None:
        body = {
            "sourceRecordId": source_record_id,
            "residentId": self.id,
            "kind": kind,
            "severity": severity,
            "occurredAt": (occurred_at or datetime.now(timezone.utc)).strftime(
                "%Y-%m-%dT%H:%M:%SZ"
            ),
            "narrative": narrative,
            "source": "MANUAL",
        }
        try:
            self._client.post("/api/v1/history-episodes", body)
        except ManaError as exc:
            if ignore_duplicates and exc.status and exc.status >= 400:
                return
            raise
        self.falls.refresh()
        self.episodes.refresh()

    def set_live_state(
        self,
        state: str,
        *,
        bed_id: str | None = None,
        monitor_key: str | None = None,
        ignore_errors: bool = True,
    ) -> None:
        bed_id = bed_id or self._bed_id_from_resident()
        if not bed_id:
            return
        body = {
            "sourceEventId": f"sdk-{self.key}-{uuid.uuid4()}",
            "monitorKey": monitor_key or f"sdk-{self.key}",
            "bedId": bed_id,
            "residentId": self.id,
            "kind": "POSTURE",
            "state": state,
            "sleeping": state == "lying",
            "occurredAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        }
        try:
            self._client.post("/internal/v1/events", body)
        except ManaError:
            if not ignore_errors:
                raise
        self.chart.refresh()

    # ── ingest interno ────────────────────────────────────────────

    def _bed_id_from_resident(self) -> str | None:
        try:
            raw = self._client.get(f"/api/v1/residents/{self.id}")
            return ((raw or {}).get("location") or {}).get("bedId")
        except ManaError:
            return None

    def _ingest_day(self, kind: str, obj: SleepDay | MobilityDay | BathroomDay) -> None:
        self._ingest_day_raw(kind, obj.day, obj.to_ingest())

    def _ingest_day_raw(
        self,
        kind: str,
        day: date,
        data: dict[str, Any],
        *,
        ignore_duplicates: bool = True,
    ) -> None:
        paths = {
            "sleep": "/internal/v1/clinical/sleep-summaries",
            "mobility": "/internal/v1/clinical/mobility-summaries",
            "bathroom": "/internal/v1/clinical/bathroom-summaries",
        }
        try:
            self._client.post(
                paths[kind],
                {
                    "sourceRecordId": f"sdk-{self.key}-{kind}-{day.isoformat()}",
                    "residentId": self.id,
                    "observedOn": day.isoformat(),
                    "data": data,
                    "source": "mana-sdk",
                },
            )
        except ManaError as exc:
            if ignore_duplicates and exc.status and exc.status >= 400:
                return
            raise

    def __repr__(self) -> str:
        return self._path


# alias histórico
ResidentChart = Resident
