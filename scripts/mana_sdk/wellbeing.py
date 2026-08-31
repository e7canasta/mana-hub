"""Generadores de resúmenes diarios — perfiles legibles para seeds y SDK."""

from __future__ import annotations

import hashlib
from datetime import date, timedelta
from typing import Callable


def _rng(key: str, day: date) -> int:
    digest = hashlib.md5(f"{key}:{day.isoformat()}".encode()).hexdigest()
    return int(digest[:8], 16)


def _pick(key: str, day: date, lo: int, hi: int) -> int:
    span = hi - lo + 1
    return lo + (_rng(key, day) % span)


ProfileFn = Callable[[str, date], dict[str, dict]]


def _care(key: str, day: date, *, total_lo: int = 22, total_hi: int = 48) -> dict[str, int]:
    total = _pick(key, day, total_lo, total_hi)
    proactive = _pick(key, day, max(12, total_lo - 4), min(total - 5, total_hi - 10))
    proactive = min(proactive, total)
    return {
        "totalMinutes": total,
        "proactiveMinutes": proactive,
        "roundsCount": _pick(key, day, 2, 3),
        "notesCount": _pick(key, day, 0, 1),
    }


def _stable(key: str, day: date) -> dict[str, dict]:
    calm = _pick(key, day, 330, 390)
    restless = _pick(key, day, 12, 38)
    awake = _pick(key, day, 10, 35)
    out_bed = _pick(key, day, 4, 18)
    return {
        "sleep": {
            "calmMinutes": calm,
            "restlessMinutes": restless,
            "awakeMinutes": awake,
            "outOfBedMinutes": out_bed,
            "bedExitCount": _pick(key, day, 0, 1),
            "wakeCount": _pick(key, day, 1, 3),
            "startedAt": f"{day.isoformat()}T22:15:00",
            "endedAt": f"{(day + timedelta(days=1)).isoformat()}T06:30:00",
        },
        "mobility": {
            "inBedMinutes": 420,
            "outOfBedMinutes": _pick(key, day, 35, 70),
            "outOfSightMinutes": _pick(key, day, 5, 20),
            "walkingMinutes": _pick(key, day, 18, 45),
            "distanceMeters": float(_pick(key, day, 90, 210)),
            "transferCount": _pick(key, day, 2, 5),
        },
        "bathroom": {
            "visitCount": _pick(key, day, 4, 7),
            "nightVisitCount": _pick(key, day, 0, 2),
            "assistedCount": 0,
            "totalMinutes": _pick(key, day, 18, 45),
        },
        "care": _care(key, day),
    }


def _restless(key: str, day: date) -> dict[str, dict]:
    base = _stable(key, day)
    base["sleep"]["restlessMinutes"] = _pick(key, day, 45, 90)
    base["sleep"]["outOfBedMinutes"] = _pick(key, day, 20, 55)
    base["sleep"]["bedExitCount"] = _pick(key, day, 2, 5)
    base["sleep"]["wakeCount"] = _pick(key, day, 3, 7)
    base["mobility"]["walkingMinutes"] = _pick(key, day, 35, 75)
    base["mobility"]["distanceMeters"] = float(_pick(key, day, 150, 320))
    base["bathroom"]["visitCount"] = _pick(key, day, 5, 9)
    base["bathroom"]["nightVisitCount"] = _pick(key, day, 1, 4)
    base["care"] = _care(key, day, total_lo=35, total_hi=70)
    return base


def _low_mobility(key: str, day: date) -> dict[str, dict]:
    base = _stable(key, day)
    base["mobility"]["walkingMinutes"] = _pick(key, day, 5, 18)
    base["mobility"]["distanceMeters"] = float(_pick(key, day, 25, 80))
    base["mobility"]["transferCount"] = _pick(key, day, 1, 3)
    base["mobility"]["outOfBedMinutes"] = _pick(key, day, 20, 45)
    return base


def _active(key: str, day: date) -> dict[str, dict]:
    base = _stable(key, day)
    base["mobility"]["walkingMinutes"] = _pick(key, day, 50, 95)
    base["mobility"]["distanceMeters"] = float(_pick(key, day, 220, 420))
    base["mobility"]["outOfBedMinutes"] = _pick(key, day, 70, 120)
    base["bathroom"]["visitCount"] = _pick(key, day, 6, 10)
    return base


PROFILES: dict[str, ProfileFn] = {
    "stable": _stable,
    "restless": _restless,
    "low_mobility": _low_mobility,
    "active": _active,
}


def date_range(days: int, *, end: date | None = None) -> list[date]:
    end = end or date.today()
    start = end - timedelta(days=days - 1)
    out: list[date] = []
    cursor = start
    while cursor <= end:
        out.append(cursor)
        cursor += timedelta(days=1)
    return out


def generate_day(profile: str, resident_key: str, day: date) -> dict[str, dict]:
    fn = PROFILES.get(profile, _stable)
    return fn(resident_key, day)
