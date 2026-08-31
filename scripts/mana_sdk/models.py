"""Modelos del chart — mapean proyecciones GET /api/v1/views/resident-chart/..."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date
from typing import Any


@dataclass
class Location:
    wing_name: str | None = None
    room_number: str | None = None
    bed_label: str | None = None

    @classmethod
    def from_api(cls, raw: dict | None) -> Location | None:
        if not raw:
            return None
        return cls(
            wing_name=raw.get("wingName"),
            room_number=raw.get("roomNumber"),
            bed_label=raw.get("bedLabel"),
        )

    def label(self) -> str:
        parts = [p for p in (self.room_number, self.bed_label) if p]
        return f"Hab. {' · '.join(parts)}" if parts else "—"


@dataclass
class LiveState:
    state: str | None = None
    staff_present: bool | None = None
    state_since: str | None = None

    @classmethod
    def from_api(cls, raw: dict | None) -> LiveState | None:
        if not raw:
            return None
        return cls(
            state=raw.get("state"),
            staff_present=raw.get("staffPresent"),
            state_since=raw.get("stateSince"),
        )


@dataclass
class ChartHeader:
    id: str
    full_name: str
    birth_date: str | None
    admission_date: str | None
    location: Location | None
    current_state: LiveState | None

    @classmethod
    def from_api(cls, raw: dict) -> ChartHeader:
        return cls(
            id=raw["id"],
            full_name=raw["fullName"],
            birth_date=raw.get("birthDate"),
            admission_date=raw.get("admissionDate"),
            location=Location.from_api(raw.get("location")),
            current_state=LiveState.from_api(raw.get("currentState")),
        )


@dataclass
class SleepDay:
    day: date
    calm_minutes: int = 0
    restless_minutes: int = 0
    awake_minutes: int = 0
    out_of_bed_minutes: int = 0
    bed_exit_count: int = 0
    wake_count: int = 0
    started_at: str | None = None
    ended_at: str | None = None
    _dirty: bool = field(default=False, repr=False)

    @classmethod
    def from_api(cls, raw: dict) -> SleepDay:
        return cls(
            day=date.fromisoformat(raw["day"]),
            calm_minutes=raw.get("calmMinutes", 0),
            restless_minutes=raw.get("restlessMinutes", 0),
            awake_minutes=raw.get("awakeMinutes", 0),
            out_of_bed_minutes=raw.get("outOfBedMinutes", 0),
            bed_exit_count=raw.get("bedExitCount", 0),
            wake_count=raw.get("wakeCount", 0),
            started_at=raw.get("startedAt"),
            ended_at=raw.get("endedAt"),
        )

    def to_ingest(self) -> dict[str, Any]:
        out: dict[str, Any] = {
            "calmMinutes": self.calm_minutes,
            "restlessMinutes": self.restless_minutes,
            "awakeMinutes": self.awake_minutes,
            "outOfBedMinutes": self.out_of_bed_minutes,
            "bedExitCount": self.bed_exit_count,
            "wakeCount": self.wake_count,
        }
        if self.started_at:
            out["startedAt"] = self.started_at
        if self.ended_at:
            out["endedAt"] = self.ended_at
        return out


@dataclass
class MobilityDay:
    day: date
    walking_minutes: int = 0
    distance_meters: float = 0.0
    transfer_count: int = 0
    out_of_bed_minutes: int = 0
    _dirty: bool = field(default=False, repr=False)

    @classmethod
    def from_api(cls, raw: dict) -> MobilityDay:
        return cls(
            day=date.fromisoformat(raw["day"]),
            walking_minutes=raw.get("walkingMinutes", 0),
            distance_meters=float(raw.get("distanceMeters", 0)),
            transfer_count=raw.get("transferCount", 0),
            out_of_bed_minutes=raw.get("outOfBedMinutes", 0),
        )

    def to_ingest(self) -> dict[str, Any]:
        return {
            "walkingMinutes": self.walking_minutes,
            "distanceMeters": self.distance_meters,
            "transferCount": self.transfer_count,
            "outOfBedMinutes": self.out_of_bed_minutes,
            "inBedMinutes": 420,
            "outOfSightMinutes": 0,
        }


@dataclass
class BathroomDay:
    day: date
    visit_count: int = 0
    night_visit_count: int = 0
    _dirty: bool = field(default=False, repr=False)

    @classmethod
    def from_api(cls, raw: dict) -> BathroomDay:
        return cls(
            day=date.fromisoformat(raw["day"]),
            visit_count=raw.get("visitCount", 0),
            night_visit_count=raw.get("nightVisitCount", 0),
        )

    def to_ingest(self) -> dict[str, Any]:
        return {
            "visitCount": self.visit_count,
            "nightVisitCount": self.night_visit_count,
            "assistedCount": 0,
            "totalMinutes": 0,
        }


@dataclass
class EpisodeItem:
    id: str
    kind: str
    severity: str
    occurred_at: str
    injury_status: str | None = None
    self_recovery: bool | None = None
    verdict: str | None = None
    review_note: str | None = None
    reviewed_at: str | None = None

    @classmethod
    def from_api(cls, raw: dict) -> EpisodeItem:
        return cls(
            id=raw["id"],
            kind=raw["kind"],
            severity=raw["severity"],
            occurred_at=raw["occurredAt"],
            injury_status=raw.get("injuryStatus"),
            self_recovery=raw.get("selfRecovery"),
            verdict=raw.get("verdict"),
            review_note=raw.get("reviewNote"),
            reviewed_at=raw.get("reviewedAt"),
        )


@dataclass
class FallsSummary:
    streak_days: int | None
    previous_streak_days: int | None
    falls_last_12_months: int
    last_fall_at: str | None
    last_fall_injury: str | None
    months: list[dict[str, Any]]

    @classmethod
    def from_api(cls, raw: dict) -> FallsSummary:
        return cls(
            streak_days=raw.get("streakDays"),
            previous_streak_days=raw.get("previousStreakDays"),
            falls_last_12_months=raw.get("fallsLast12Months", 0),
            last_fall_at=raw.get("lastFallAt"),
            last_fall_injury=raw.get("lastFallInjury"),
            months=raw.get("months", []),
        )


@dataclass
class AlarmPresets:
    """Editable — PATCH /api/v1/alarm-presets/{id}"""

    risk_level: str | None = None
    mobility_aid: str | None = None
    autopilot: bool | None = None
    mode: str | None = None
    template_id: str | None = None
    overrides: dict[str, Any] = field(default_factory=dict)
    updated_by: str | None = None
    reason: str | None = None
    _dirty: bool = field(default=False, repr=False)

    @classmethod
    def from_api(cls, raw: dict) -> AlarmPresets:
        return cls(
            risk_level=raw.get("riskLevel"),
            mobility_aid=raw.get("mobilityAid"),
            autopilot=raw.get("autopilot"),
            mode=raw.get("mode"),
            template_id=raw.get("templateId"),
            overrides=dict(raw.get("overrides") or {}),
            updated_by=raw.get("updatedBy"),
        )

    def apply(self, spec: dict) -> None:
        """Aplica un dict estilo seed (snake_case + overrides anidados)."""
        if "risk_level" in spec:
            self.risk_level = spec["risk_level"]
        if "mobility_aid" in spec:
            self.mobility_aid = spec["mobility_aid"]
        if "autopilot" in spec:
            self.autopilot = spec["autopilot"]
        if "mode" in spec:
            self.mode = spec["mode"]
        if "template_id" in spec:
            self.template_id = spec["template_id"]
        if "overrides" in spec:
            self.overrides = dict(spec["overrides"])
        if "reason" in spec:
            self.reason = spec["reason"]
        self._dirty = True

    def update(self, **fields: Any) -> None:
        """Atajos camel/snake: update(risk_level='high') marca dirty."""
        mapping = {
            "riskLevel": "risk_level",
            "mobilityAid": "mobility_aid",
            "templateId": "template_id",
        }
        for key, value in fields.items():
            attr = mapping.get(key, key)
            if hasattr(self, attr):
                setattr(self, attr, value)
        self._dirty = True

    def to_patch_body(self) -> dict[str, Any]:
        import json

        body: dict[str, Any] = {}
        if self.risk_level is not None:
            body["riskLevel"] = self.risk_level
        if self.mobility_aid is not None:
            body["mobilityAid"] = self.mobility_aid
        if self.autopilot is not None:
            body["autopilot"] = self.autopilot
        if self.mode is not None:
            body["mode"] = self.mode
        if self.template_id is not None:
            body["templateId"] = self.template_id
        if self.overrides:
            body["overridesJson"] = json.dumps(self.overrides)
        body["updatedBy"] = self.updated_by or "mana-sdk"
        if self.reason:
            body["reason"] = self.reason
        return body
