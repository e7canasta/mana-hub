#!/usr/bin/env python3
"""
Resetea la DB y siembra José E1 desde cero.

  1. TRUNCATE tablas (conserva schema Flyway)
  2. SQL bootstrap: facility + Hab 101 + jose (IDs fijos)
  3. mana-sdk: alarmas E1 + 14 d summaries + historia

Requisitos:
  - PostgreSQL en localhost:5432 (docker compose up -d)
  - psql en PATH
  - mana-hub corriendo en :8080

Uso:
  python scripts/reset_and_seed.py
  python scripts/reset_and_seed.py --verify
  DATABASE_URL=postgresql://... python scripts/reset_and_seed.py
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from mana_sdk import ManaHub
from mana_sdk.db import DbError, wipe_and_bootstrap, DEFAULT_DSN
from mana_sdk.scenario import apply_resident_spec
from seed.scenarios import demo_panel as scenario


def main() -> int:
    parser = argparse.ArgumentParser(description="Reset DB + seed José E1")
    parser.add_argument("--db-url", default=DEFAULT_DSN, help="PostgreSQL connection URL")
    parser.add_argument("--hub-url", default="http://localhost:8080", help="mana-hub base URL")
    parser.add_argument("--verify", action="store_true", help="Verificar proyecciones al final")
    parser.add_argument("--skip-db", action="store_true", help="Solo seed API (DB ya vacía)")
    args = parser.parse_args()

    if not args.skip_db:
        print("[1/3] Reset DB + bootstrap José…")
        try:
            wipe_and_bootstrap(dsn=args.db_url)
        except DbError as exc:
            print(f"✗ {exc}")
            return 1
        print("  ✓ DB vacía + jose / hab 101")

    hub = ManaHub(args.hub_url)
    if not hub.alive():
        print(f"✗ mana-hub no responde en {args.hub_url}")
        return 1

    print("[2/3] Seed chart vía API…")
    for spec in scenario.RESIDENTS:
        resident = hub.residents.get(spec["key"], hydrate=False)
        stats = apply_resident_spec(
            resident,
            spec,
            wellbeing_days=spec.get("wellbeing_days", scenario.WELLBEING_DAYS),
        )
        print(
            f"  ✓ {spec['key']}: alarms={stats['alarms']} "
            f"wellbeing={stats['wellbeing_days']}d history={stats['history']}"
        )

    if args.verify:
        print("[3/3] Verificar proyecciones…")
        for spec in scenario.RESIDENTS:
            r = hub.residents.get(spec["key"])
            loc = r.location.label() if r.location else "—"
            state = r.current_state.state if r.current_state else "—"
            print(
                f"  ✓ {r.full_name}: {loc} | state={state} | "
                f"sleep={len(r.sleep)}d | alarm={r.alarm.risk_level}"
            )
    else:
        print("[3/3] Listo (usá --verify para comprobar proyecciones)")

    print("\n✓ Base demo: José (E1) + Susan (alta reciente)")
    print("  Editar escenario: scripts/seed/scenarios/demo_panel.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
