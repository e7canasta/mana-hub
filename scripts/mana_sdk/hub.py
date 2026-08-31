"""Repositorio de residentes."""

from __future__ import annotations

from .client import ManaClient
from .resident import Resident


class Residents:
    def __init__(self, client: ManaClient):
        self._client = client
        self._index: list[dict] | None = None

    def _load_index(self) -> list[dict]:
        if self._index is None:
            self._index = self._client.get("/api/v1/residents") or []
        return self._index

    def list(self, *, refresh: bool = False) -> list[Resident]:
        if refresh:
            self._index = None
        return [
            Resident(self._client, r["id"], key=r.get("externalId") or r["id"])
            for r in self._load_index()
        ]

    def get(self, key: str, *, hydrate: bool = True, wellbeing_days: int = 14) -> Resident:
        row = self._find(key)
        if row is None:
            raise LookupError(f"Resident not found: {key!r}")
        resident = Resident(self._client, row["id"], key=key)
        if hydrate:
            resident.refresh(wellbeing_days=wellbeing_days)
        return resident

    def find(self, key: str) -> Resident | None:
        row = self._find(key)
        if row is None:
            return None
        return Resident(self._client, row["id"], key=key)

    def _find(self, key: str) -> dict | None:
        for r in self._load_index():
            if r.get("id") == key or r.get("externalId") == key:
                return r
        return None
