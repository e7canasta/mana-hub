"""
mana-sdk — DSL semántico sobre proyecciones del panel.

Árbol de navegación:

    ManaHub
      └── residents.get("jose")
            ├── chart          → identidad, ubicación, estado vivo
            ├── wellbeing
            │     ├── sleep    → días editables + avg_calm_minutes()
            │     ├── mobility
            │     └── bathroom
            ├── alarms         → presets editables (PATCH)
            ├── falls          → streak, meses (lectura)
            └── episodes       → listado; .falls() filtra FALL

Ejemplo:

    jose = hub.residents.get("jose")
    print(jose.chart.location.label())
    print(jose.wellbeing.sleep.avg_calm_minutes())

    night = jose.wellbeing.sleep["2026-08-30"]
    night.calm_minutes = 400

    jose.alarms.risk_level = "high"
    jose.push()
"""

from .client import ManaClient, ManaError
from .hub import Residents
from .models import (
    AlarmPresets,
    BathroomDay,
    ChartHeader,
    EpisodeItem,
    FallsSummary,
    LiveState,
    Location,
    MobilityDay,
    SleepDay,
)
from .resident import Resident, ResidentChart
from .views import (
    AlarmsView,
    ChartView,
    DayRef,
    EpisodesView,
    FallsView,
    WellbeingView,
)
from . import wellbeing, db, scenario


class ManaHub:
    def __init__(self, base_url: str = "http://localhost:8080", *, timeout: float = 30.0):
        self.client = ManaClient(base_url, timeout=timeout)
        self.residents = Residents(self.client)

    def alive(self) -> bool:
        return self.client.alive()


__all__ = [
    "ManaHub",
    "ManaClient",
    "ManaError",
    "Resident",
    "ResidentChart",
    "Residents",
    "ChartView",
    "WellbeingView",
    "AlarmsView",
    "FallsView",
    "EpisodesView",
    "DayRef",
    "AlarmPresets",
    "SleepDay",
    "MobilityDay",
    "BathroomDay",
    "EpisodeItem",
    "FallsSummary",
    "ChartHeader",
    "Location",
    "LiveState",
    "wellbeing",
    "db",
    "scenario",
]
