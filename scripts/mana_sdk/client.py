"""HTTP client para mana-hub (stdlib only)."""

from __future__ import annotations

import json
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Any


@dataclass
class ManaClient:
    base_url: str = "http://localhost:8080"
    timeout: float = 30.0

    def __post_init__(self) -> None:
        self.base_url = self.base_url.rstrip("/")

    def alive(self) -> bool:
        try:
            self.get("/api/v1/facilities")
            return True
        except ManaError:
            return False

    def get(self, path: str) -> Any:
        return self._request("GET", path)

    def post(self, path: str, body: dict | None = None) -> Any:
        return self._request("POST", path, body)

    def patch(self, path: str, body: dict | None = None) -> Any:
        return self._request("PATCH", path, body)

    def _request(self, method: str, path: str, body: dict | None = None) -> Any:
        url = f"{self.base_url}{path}"
        data = None
        headers = {"Accept": "application/json"}
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"

        req = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                raw = resp.read().decode("utf-8")
                if not raw:
                    return None
                return json.loads(raw)
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise ManaError(method, path, exc.code, detail) from exc
        except urllib.error.URLError as exc:
            raise ManaError(method, path, None, str(exc.reason)) from exc


class ManaError(Exception):
    def __init__(self, method: str, path: str, status: int | None, detail: str):
        self.method = method
        self.path = path
        self.status = status
        self.detail = detail
        super().__init__(f"{method} {path} -> {status}: {detail[:300]}")
