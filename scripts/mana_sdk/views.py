"""Vistas navegables del DSL — sabés en qué nodo estás parado."""

from __future__ import annotations

from datetime import date
from typing import Any, Generic, Iterator, TypeVar

from . import wellbeing
from .models import (
    AlarmPresets,
    BathroomDay,
    ChartHeader,
    EpisodeItem,
    FallsSummary,
    MobilityDay,
    SleepDay,
)

TDay = TypeVar("TDay", SleepDay, MobilityDay, BathroomDay)


class _Node:
    """Base con ruta semántica para repr y debugging."""

    _path: str = "mana"

    def __repr__(self) -> str:
        return self._path


class DayRef(_Node, Generic[TDay]):
    """Un día editable — asignar un campo marca dirty automáticamente."""

    _FIELD_NAMES: frozenset[str] = frozenset()

    def __init__(self, path: str, obj: TDay, on_dirty: Any):
        self._path = path
        self._obj = obj
        self._on_dirty = on_dirty

    def __getattr__(self, name: str) -> Any:
        if name.startswith("_") or name in ("_path", "_obj", "_on_dirty"):
            raise AttributeError(name)
        return getattr(self._obj, name)

    def __setattr__(self, name: str, value: Any) -> None:
        if name.startswith("_") or name in ("_path", "_obj", "_on_dirty"):
            super().__setattr__(name, value)
            return
        setattr(self._obj, name, value)
        self._obj._dirty = True
        self._on_dirty()

    @property
    def day(self) -> date:
        return self._obj.day


class SleepDayRef(DayRef[SleepDay]):
    _FIELD_NAMES = frozenset(
        {
            "calm_minutes",
            "restless_minutes",
            "awake_minutes",
            "out_of_bed_minutes",
            "bed_exit_count",
            "wake_count",
            "started_at",
            "ended_at",
        }
    )


class MobilityDayRef(DayRef[MobilityDay]):
    _FIELD_NAMES = frozenset(
        {"walking_minutes", "distance_meters", "transfer_count", "out_of_bed_minutes"}
    )


class BathroomDayRef(DayRef[BathroomDay]):
    _FIELD_NAMES = frozenset({"visit_count", "night_visit_count"})


class _Dimension(_Node):
    kind: str = ""
    _day_type: type
    _ref_type: type

    def __init__(self, resident: Any, path: str):
        self._resident = resident
        self._path = path
        self._days: dict[date, TDay] = {}
        self._range: tuple[date, date] | None = None

    def __len__(self) -> int:
        return len(self._days)

    def __iter__(self) -> Iterator[DayRef]:
        for d in sorted(self._days):
            yield self[d]

    def __getitem__(self, day: date | str) -> DayRef:
        key = date.fromisoformat(day) if isinstance(day, str) else day
        if key not in self._days:
            self._days[key] = self._day_type(day=key)  # type: ignore[call-arg]
        return self._ref_type(
            f"{self._path}[{key.isoformat()}]",
            self._days[key],
            self._mark_dirty,
        )

    def _mark_dirty(self) -> None:
        self._resident._dirty_wellbeing.add(self.kind)

    def _load(self, *, days: int, end: date | None) -> None:
        dr = wellbeing.date_range(days, end=end)
        self._range = (dr[0], dr[-1])
        raw = self._resident._client.get(
            f"/api/v1/views/resident-chart/{self._resident.id}/{self.kind}"
            f"?from={dr[0].isoformat()}&to={dr[-1].isoformat()}"
        )
        self._days = {}
        for row in (raw or {}).get("summaries", []):
            obj = self._day_type.from_api(row)
            self._days[obj.day] = obj

    def ingest_dirty(self) -> int:
        count = 0
        for obj in self._days.values():
            if not obj._dirty:
                continue
            self._resident._ingest_day(self.kind, obj)
            obj._dirty = False
            count += 1
        return count


class SleepDimension(_Dimension):
    kind = "sleep"
    _day_type = SleepDay
    _ref_type = SleepDayRef

    def avg_calm_minutes(self) -> float | None:
        if not self._days:
            return None
        return sum(d.calm_minutes for d in self._days.values()) / len(self._days)

    def avg_out_of_bed_minutes(self) -> float | None:
        if not self._days:
            return None
        return sum(d.out_of_bed_minutes for d in self._days.values()) / len(self._days)

    def avg_bed_exits(self) -> float | None:
        if not self._days:
            return None
        return sum(d.bed_exit_count for d in self._days.values()) / len(self._days)


class MobilityDimension(_Dimension):
    kind = "mobility"
    _day_type = MobilityDay
    _ref_type = MobilityDayRef

    def avg_walking_minutes(self) -> float | None:
        if not self._days:
            return None
        return sum(d.walking_minutes for d in self._days.values()) / len(self._days)


class BathroomDimension(_Dimension):
    kind = "bathroom"
    _day_type = BathroomDay
    _ref_type = BathroomDayRef

    def avg_visits(self) -> float | None:
        if not self._days:
            return None
        return sum(d.visit_count for d in self._days.values()) / len(self._days)


class ChartView(_Node):
    def __init__(self, resident: Any):
        self._resident = resident
        self._path = f"mana.residents[{resident.key!r}].chart"
        self._data: ChartHeader | None = None

    def refresh(self) -> ChartView:
        raw = self._resident._client.get(
            f"/api/v1/views/resident-chart/{self._resident.id}"
        )
        self._data = ChartHeader.from_api(raw) if raw else None
        return self

    @property
    def full_name(self) -> str:
        return self._data.full_name if self._data else self._resident.key

    @property
    def birth_date(self) -> str | None:
        return self._data.birth_date if self._data else None

    @property
    def admission_date(self) -> str | None:
        return self._data.admission_date if self._data else None

    @property
    def location(self):
        return self._data.location if self._data else None

    @property
    def live_state(self):
        return self._data.current_state if self._data else None

    # alias panel
    current_state = live_state


class WellbeingView(_Node):
    def __init__(self, resident: Any):
        self._resident = resident
        base = f"mana.residents[{resident.key!r}].wellbeing"
        self._path = base
        self.sleep = SleepDimension(resident, f"{base}.sleep")
        self.mobility = MobilityDimension(resident, f"{base}.mobility")
        self.bathroom = BathroomDimension(resident, f"{base}.bathroom")

    def load(self, *, days: int = 14, end: date | None = None) -> WellbeingView:
        self.sleep._load(days=days, end=end)
        self.mobility._load(days=days, end=end)
        self.bathroom._load(days=days, end=end)
        return self

    def push(self) -> dict[str, int]:
        return {
            "sleep": self.sleep.ingest_dirty(),
            "mobility": self.mobility.ingest_dirty(),
            "bathroom": self.bathroom.ingest_dirty(),
        }


class AlarmsView(_Node):
    """Presets editables — PATCH /api/v1/alarm-presets/{id}."""

    def __init__(self, resident: Any):
        self._resident = resident
        self._path = f"mana.residents[{resident.key!r}].alarms"
        self._data = AlarmPresets()

    def refresh(self) -> AlarmsView:
        raw = self._resident._client.get(
            f"/api/v1/views/resident-chart/{self._resident.id}/alarm-presets"
        )
        if raw:
            self._data = AlarmPresets.from_api(raw)
        return self

    def __getattr__(self, name: str) -> Any:
        if name.startswith("_"):
            raise AttributeError(name)
        return getattr(self._data, name)

    def __setattr__(self, name: str, value: Any) -> None:
        if name.startswith("_") or name in ("_resident", "_path", "_data"):
            super().__setattr__(name, value)
            return
        setattr(self._data, name, value)
        self._data._dirty = True

    def update(self, **fields: Any) -> AlarmsView:
        self._data.update(**fields)
        return self

    def apply(self, spec: dict) -> AlarmsView:
        self._data.apply(spec)
        return self

    def push(self) -> AlarmsView:
        body = self._data.to_patch_body()
        self._resident._client.patch(f"/api/v1/alarm-presets/{self._resident.id}", body)
        self.refresh()
        self._data._dirty = False
        return self

    @property
    def overrides(self) -> dict[str, Any]:
        return self._data.overrides

    @overrides.setter
    def overrides(self, value: dict[str, Any]) -> None:
        self._data.overrides = dict(value)
        self._data._dirty = True


class FallsView(_Node):
    def __init__(self, resident: Any):
        self._resident = resident
        self._path = f"mana.residents[{resident.key!r}].falls"
        self._data: FallsSummary | None = None

    def refresh(self) -> FallsView:
        raw = self._resident._client.get(
            f"/api/v1/views/resident-chart/{self._resident.id}/falls"
        )
        self._data = FallsSummary.from_api(raw) if raw else None
        return self

    def __getattr__(self, name: str) -> Any:
        if self._data is None:
            raise AttributeError(f"{name} — call refresh() first")
        # snake_case en Python desde camelCase API ya mapeado en FallsSummary
        return getattr(self._data, name)


class EpisodesView(_Node):
    def __init__(self, resident: Any):
        self._resident = resident
        self._path = f"mana.residents[{resident.key!r}].episodes"
        self._items: list[EpisodeItem] = []

    def refresh(self) -> EpisodesView:
        raw = self._resident._client.get(
            f"/api/v1/views/resident-chart/{self._resident.id}/episodes"
        )
        self._items = [
            EpisodeItem.from_api(row) for row in (raw or {}).get("episodes", [])
        ]
        return self

    def __len__(self) -> int:
        return len(self._items)

    def __iter__(self) -> Iterator[EpisodeItem]:
        return iter(self._items)

    def __getitem__(self, index: int) -> EpisodeItem:
        return self._items[index]

    def falls(self) -> list[EpisodeItem]:
        return [e for e in self._items if e.kind == "FALL"]
