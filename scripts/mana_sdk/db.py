"""Utilidades de base de datos para dev."""

from __future__ import annotations

import os
import shutil
import subprocess
from pathlib import Path

DB_DIR = Path(__file__).resolve().parent.parent / "db"
DEFAULT_DSN = os.environ.get(
    "DATABASE_URL",
    "postgresql://postgres:postgres@localhost:5432/mana_hub",
)
DOCKER_PG_CONTAINER = os.environ.get("MANA_PG_CONTAINER", "")
DOCKER_PG_CANDIDATES = ("mana-hub-pg", "mana-pg-dev")


class DbError(Exception):
    pass


def _resolve_pg_container() -> str | None:
    if DOCKER_PG_CONTAINER:
        return DOCKER_PG_CONTAINER
    if not shutil.which("docker"):
        return None
    try:
        lines = subprocess.run(
            ["docker", "ps", "--format", "{{.Names}}"],
            capture_output=True,
            text=True,
            check=True,
        ).stdout.splitlines()
        for name in DOCKER_PG_CANDIDATES:
            if name in lines:
                return name
        for name in lines:
            if "pg" in name.lower() or "postgres" in name.lower():
                return name
    except subprocess.CalledProcessError:
        return None
    return None


def _run_sql_file(path: Path, *, dsn: str) -> None:
    if shutil.which("psql"):
        try:
            subprocess.run(
                ["psql", dsn, "-v", "ON_ERROR_STOP=1", "-f", str(path)],
                check=True,
                text=True,
            )
            return
        except subprocess.CalledProcessError as exc:
            raise DbError(f"psql falló (exit {exc.returncode})") from exc

    container = _resolve_pg_container()
    if container:
        try:
            with path.open("r", encoding="utf-8") as fh:
                subprocess.run(
                    [
                        "docker",
                        "exec",
                        "-i",
                        container,
                        "psql",
                        "-U",
                        "postgres",
                        "-d",
                        "mana_hub",
                        "-v",
                        "ON_ERROR_STOP=1",
                    ],
                    stdin=fh,
                    check=True,
                    text=True,
                )
            return
        except subprocess.CalledProcessError as exc:
            raise DbError(f"docker exec psql falló (exit {exc.returncode})") from exc

    raise DbError(
        "No hay psql local ni contenedor Postgres. "
        "Levantá la DB: docker compose up -d "
        "(o MANA_PG_CONTAINER=nombre-contenedor)"
    )


def reset(dsn: str = DEFAULT_DSN) -> None:
    """TRUNCATE de todas las tablas excepto flyway_schema_history."""
    _run_sql_file(DB_DIR / "reset.sql", dsn=dsn)


def bootstrap_jose(dsn: str = DEFAULT_DSN) -> None:
    """Inserta facility + José + Hab 101 con IDs fijos."""
    _run_sql_file(DB_DIR / "bootstrap_jose.sql", dsn=dsn)


def wipe_and_bootstrap(dsn: str = DEFAULT_DSN) -> None:
    reset(dsn=dsn)
    bootstrap_jose(dsn=dsn)
