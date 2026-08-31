#!/usr/bin/env python3
"""
Seed legible para pruebas del panel (ResumenTab / resident-chart).

Todo pasa por la API de mana-hub — sin SQL ni Gradle.

Uso:
  # Hub corriendo en :8080
  python scripts/seed/run.py                    # José E1 — una habitación (demo_panel.py)
  python scripts/seed/run.py --chart-only       # solo summaries + falls + estado
  python scripts/seed/run.py --chart-only --residents jose
  python scripts/seed/run.py --verify           # seed + GET proyecciones
  python scripts/reset_and_seed.py              # DB vacía + José E1 desde cero
  python scripts/reset_and_seed.py --verify

Editá los datos en:
  scripts/seed/scenarios/demo_panel.py
"""

from __future__ import annotations

import argparse
import json
import sys
import uuid
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from typing import Any

# Permite `python scripts/seed/run.py` sin instalar paquete
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from seed.hub_client import HubClient, HubError
from seed import wellbeing
from seed.scenarios import demo_panel as scenario


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _iso_instant(dt: datetime) -> str:
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def _days_ago(days: int) -> datetime:
    return _utc_now() - timedelta(days=days)


# ── Facility registry ─────────────────────────────────────────────


def find_facility_by_name(client: HubClient, name: str) -> dict | None:
    facilities = client.get("/api/v1/facilities") or []
    return next((f for f in facilities if f.get("name") == name), None)


def build_room_registry(tree: dict) -> dict[tuple[str, str], dict]:
    """(wing_name, room_number) → {room_id, beds: {label → bed_id, monitor_key}}"""
    registry: dict[tuple[str, str], dict] = {}
    for wing_node in tree.get("wings", []):
        wing_name = wing_node["wing"]["name"]
        for room_node in wing_node.get("rooms", []):
            room = room_node["room"]
            beds = {
                b["label"]: {"bed_id": b["id"], "monitor_key": b.get("monitorKey")}
                for b in room_node.get("beds", [])
            }
            registry[(wing_name, room["number"])] = {
                "room_id": room["id"],
                "beds": beds,
            }
    return registry


def _admission_date(spec: dict) -> str:
    if "admission_days_ago" in spec:
        return (_utc_now() - timedelta(days=spec["admission_days_ago"])).date().isoformat()
    return spec.get("admission_date", scenario.ADMISSION_DATE)


def sync_facility_layout(client: HubClient, facility_id: str, *, dry_run: bool) -> dict:
    """Crea habitaciones/camas del escenario que aún no existen (p.ej. Hab. 102 para Susan)."""
    tree = client.get(f"/api/v1/facilities/{facility_id}/tree")
    registry = build_room_registry(tree)
    for wing_def in scenario.FACILITY["wings"]:
        wing_node = next(
            (w for w in tree.get("wings", []) if w.get("wing", {}).get("name") == wing_def["name"]),
            None,
        )
        if not wing_node:
            continue
        wing_id = wing_node["wing"]["id"]
        for room_def in wing_def["rooms"]:
            key = (wing_def["name"], room_def["number"])
            if key in registry:
                continue
            if dry_run:
                print(f"  → would add room {room_def['number']} in {wing_def['name']}")
                continue
            room = client.post(
                f"/api/v1/wings/{wing_id}/rooms",
                {"number": room_def["number"]},
            )
            room_id = room["id"]
            beds: dict = {}
            for bed_def in room_def["beds"]:
                bed = client.post(
                    f"/api/v1/rooms/{room_id}/beds",
                    {"label": bed_def["label"], "monitorKey": bed_def.get("monitor_key")},
                )
                beds[bed_def["label"]] = {
                    "bed_id": bed["id"],
                    "monitor_key": bed_def.get("monitor_key"),
                }
            registry[key] = {"room_id": room_id, "beds": beds}
            print(f"  ✓ room {room_def['number']} added ({wing_def['name']})")
    return registry


def ensure_facility(client: HubClient, dry_run: bool) -> tuple[str, dict]:
    existing = find_facility_by_name(client, scenario.FACILITY["name"])
    if existing:
        print(f"  ✓ facility exists: {existing['id']} ({existing['name']})")
        registry = sync_facility_layout(client, existing["id"], dry_run=dry_run)
        return existing["id"], registry

    if dry_run:
        print(f"  → would create facility: {scenario.FACILITY['name']}")
        return "dry-facility", {}

    resp = client.post(
        "/api/v1/facilities",
        {"name": scenario.FACILITY["name"], "timezone": scenario.FACILITY["timezone"]},
    )
    facility_id = resp["id"]
    print(f"  ✓ facility created: {facility_id}")

    for wing_def in scenario.FACILITY["wings"]:
        wing = client.post(
            f"/api/v1/facilities/{facility_id}/wings",
            {"name": wing_def["name"], "floor": wing_def.get("floor"), "sortOrder": 0},
        )
        wing_id = wing["id"]
        for room_def in wing_def["rooms"]:
            room = client.post(
                f"/api/v1/wings/{wing_id}/rooms",
                {"number": room_def["number"]},
            )
            room_id = room["id"]
            for bed_def in room_def["beds"]:
                client.post(
                    f"/api/v1/rooms/{room_id}/beds",
                    {"label": bed_def["label"], "monitorKey": bed_def.get("monitor_key")},
                )

    tree = client.get(f"/api/v1/facilities/{facility_id}/tree")
    return facility_id, build_room_registry(tree)


# ── Residents ─────────────────────────────────────────────────────


def list_residents(client: HubClient) -> list[dict]:
    return client.get("/api/v1/residents") or []


def resolve_resident_id(residents: list[dict], key: str) -> str | None:
    for r in residents:
        if r.get("id") == key or r.get("externalId") == key:
            return r["id"]
    return None


def ensure_resident(
    client: HubClient,
    spec: dict,
    *,
    dry_run: bool,
    assign: bool,
    room_registry: dict,
) -> str | None:
    existing = resolve_resident_id(list_residents(client), spec["key"])
    if existing:
        print(f"  ✓ resident {spec['key']} → {existing}")
        resident_id = existing
    elif dry_run:
        print(f"  → would create resident: {spec['key']} ({spec['full_name']})")
        resident_id = f"dry-{spec['key']}"
    else:
        created = client.post(
            "/api/v1/residents",
            {
                "fullName": spec["full_name"],
                "birthDate": spec.get("birth_date"),
                "admissionDate": _admission_date(spec),
                "externalId": spec["key"],
            },
        )
        resident_id = created["id"]
        print(f"  ✓ resident {spec['key']} → {resident_id}")

    if assign and "room" in spec:
        loc = (spec["wing"], spec["room"])
        bed_label = spec["bed_label"]
        bed_id = room_registry.get(loc, {}).get("beds", {}).get(bed_label, {}).get("bed_id")
        if not bed_id:
            if dry_run:
                print(f"    → would assign {spec['key']} to {loc} {bed_label}")
            else:
                print(f"    ⚠ bed not found for {spec['key']} at {loc} {bed_label}")
            return resident_id

        if dry_run:
            print(f"    → would assign {spec['key']} → bed {bed_id}")
            return resident_id

        # Evitar doble asignación abierta
        assignments = client.get(f"/api/v1/residents/{resident_id}/assignments") or []
        open_assign = next((a for a in assignments if a.get("isOpen")), None)
        if open_assign and open_assign.get("bedId") == bed_id:
            print(f"    ✓ already assigned to {bed_id}")
        elif open_assign:
            print(f"    ⚠ {spec['key']} already assigned to {open_assign['bedId']}, skip")
        else:
            client.post(f"/api/v1/residents/{resident_id}/assignments", {"bedId": bed_id})
            print(f"    ✓ assigned to {bed_id}")

    return resident_id


# ── Chart data (estado, alarmas, wellbeing, historia) ─────────────


def seed_bed_state(
    client: HubClient,
    spec: dict,
    resident_id: str,
    room_registry: dict,
    *,
    dry_run: bool,
) -> None:
    loc = (spec["wing"], spec["room"])
    bed = room_registry.get(loc, {}).get("beds", {}).get(spec["bed_label"])
    if not bed:
        return

    body = {
        "sourceEventId": f"seed-state-{spec['key']}-{uuid.uuid4()}",
        "monitorKey": bed["monitor_key"] or f"seed-{spec['key']}",
        "bedId": bed["bed_id"],
        "residentId": resident_id,
        "kind": "POSTURE",
        "state": spec["state"],
        "sleeping": spec["state"] == "lying",
        "occurredAt": _iso_instant(_utc_now()),
    }
    if dry_run:
        print(f"    → bed state {spec['key']}: {spec['state']}")
        return
    try:
        client.post("/internal/v1/events", body)
    except HubError as exc:
        # El estado vivo suele existir ya (seed Kotlin / perception previa)
        print(f"    ⚠ bed state skip ({exc.status}): {spec['state']}")


def seed_alarm(
    client: HubClient,
    spec: dict,
    resident_id: str,
    *,
    dry_run: bool,
) -> None:
    alarm = spec.get("alarm")
    if not alarm:
        return
    overrides = alarm.get("overrides")
    body = {
        "riskLevel": alarm.get("risk_level"),
        "mobilityAid": alarm.get("mobility_aid"),
        "autopilot": alarm.get("autopilot"),
        "mode": alarm.get("mode"),
        "templateId": alarm.get("template_id"),
        "updatedBy": "seed",
        "reason": alarm.get("reason", "demo seed"),
    }
    if overrides:
        body["overridesJson"] = json.dumps(overrides)
    body = {k: v for k, v in body.items() if v is not None}
    if dry_run:
        print(f"    → alarm {spec['key']}: {alarm.get('risk_level')} (E1 overrides={bool(overrides)})")
        return
    try:
        client.patch(f"/api/v1/alarm-presets/{resident_id}", body)
    except HubError as exc:
        if exc.status in (404, 500):
            print(f"    ⚠ alarm skip {spec['key']} ({exc.status})")
        else:
            raise


def existing_sleep_days(client: HubClient, resident_id: str, from_d: date, to_d: date) -> set[str]:
    try:
        proj = client.get(
            f"/api/v1/views/resident-chart/{resident_id}/sleep"
            f"?from={from_d.isoformat()}&to={to_d.isoformat()}"
        )
        return {s["day"] for s in (proj or {}).get("summaries", [])}
    except HubError:
        return set()


def _fall_dates(spec: dict) -> set[date]:
    return {
        (_utc_now() - timedelta(days=fall["days_ago"])).date()
        for fall in spec.get("falls", [])
    }


def _care_for_day(spec: dict, resident_key: str, day: date, base: dict) -> dict:
    """Mezcla rondas (proactivo) + pico reactivo en días con caída."""
    care = dict(base)
    if day in _fall_dates(spec):
        care["totalMinutes"] = care["totalMinutes"] + 25
        care["notesCount"] = max(care["notesCount"], 1)
    return care


def seed_wellbeing(
    client: HubClient,
    spec: dict,
    resident_id: str,
    *,
    days: int,
    dry_run: bool,
) -> None:
    profile = spec.get("wellbeing", "stable")
    span = spec.get("wellbeing_days", days)
    day_list = wellbeing.date_range(span)
    existing = set() if dry_run else existing_sleep_days(client, resident_id, day_list[0], day_list[-1])
    created = skipped = care_created = 0

    for day in day_list:
        payload = wellbeing.generate_day(profile, spec["key"], day)
        suffix = day.isoformat()
        already = day.isoformat() in existing

        if already:
            skipped += 1
        elif dry_run:
            created += 1
        else:
            client.post(
                "/internal/v1/clinical/sleep-summaries",
                {
                    "sourceRecordId": f"seed-{spec['key']}-sleep-{suffix}",
                    "residentId": resident_id,
                    "observedOn": suffix,
                    "data": payload["sleep"],
                    "source": "seed",
                    "modelVersion": "demo-1",
                },
            )
            client.post(
                "/internal/v1/clinical/mobility-summaries",
                {
                    "sourceRecordId": f"seed-{spec['key']}-mobility-{suffix}",
                    "residentId": resident_id,
                    "observedOn": suffix,
                    "data": payload["mobility"],
                    "source": "seed",
                },
            )
            client.post(
                "/internal/v1/clinical/bathroom-summaries",
                {
                    "sourceRecordId": f"seed-{spec['key']}-bathroom-{suffix}",
                    "residentId": resident_id,
                    "observedOn": suffix,
                    "data": payload["bathroom"],
                    "source": "seed",
                },
            )
            created += 1

        if dry_run:
            care_created += 1
            continue

        if spec.get("skip_care"):
            continue

        care = _care_for_day(spec, spec["key"], day, payload["care"])
        try:
            client.post(
                "/internal/v1/care-summaries",
                {
                    "sourceRecordId": f"seed-{spec['key']}-care-{suffix}",
                    "residentId": resident_id,
                    "observedOn": suffix,
                    "totalMinutes": care["totalMinutes"],
                    "proactiveMinutes": care["proactiveMinutes"],
                    "roundsCount": care["roundsCount"],
                    "notesCount": care["notesCount"],
                    "source": "seed",
                    "modelVersion": "demo-1",
                },
            )
            care_created += 1
        except HubError as exc:
            if exc.status not in (409, 500):
                raise

    label = "would seed" if dry_run else "seeded"
    print(
        f"    ✓ wellbeing {spec['key']}: {label} {created} clinical days "
        f"({skipped} skipped) · care {care_created} days"
    )


def seed_history(
    client: HubClient,
    spec: dict,
    resident_id: str,
    *,
    dry_run: bool,
) -> None:
    episodes: list[dict[str, Any]] = []

    for fall in spec.get("falls", []):
        episodes.append(
            {
                "sourceRecordId": f"seed-{spec['key']}-fall-{fall['days_ago']}",
                "kind": "FALL",
                "severity": "CRITICAL",
                "occurredAt": _iso_instant(_days_ago(fall["days_ago"])),
                "narrative": fall.get("narrative"),
            }
        )

    mon = spec.get("monitoring_since")
    if mon:
        episodes.append(
            {
                "sourceRecordId": f"seed-{spec['key']}-monitoring",
                "kind": mon.get("kind", "OTHER"),
                "severity": "INFO",
                "occurredAt": _iso_instant(_days_ago(mon["days_ago"])),
                "narrative": mon.get("narrative"),
            }
        )

    for ep in episodes:
        body = {
            "sourceRecordId": ep["sourceRecordId"],
            "residentId": resident_id,
            "kind": ep["kind"],
            "severity": ep["severity"],
            "occurredAt": ep["occurredAt"],
            "narrative": ep.get("narrative"),
            "source": "MANUAL",
        }
        if dry_run:
            print(f"    → history {spec['key']}: {ep['kind']} @ {ep['occurredAt']}")
            continue
        try:
            client.post("/api/v1/history-episodes", body)
        except HubError as exc:
            if exc.status == 409 or "duplicate" in exc.detail.lower() or "unique" in exc.detail.lower():
                continue
            # Al re-correr, source_record_id duplicado → ignorar silenciosamente si 500 por constraint
            if exc.status and exc.status >= 400:
                print(f"    ⚠ history skip {ep['sourceRecordId']}: {exc.status}")
                continue
            raise


def seed_resident_chart(
    client: HubClient,
    spec: dict,
    resident_id: str,
    room_registry: dict,
    *,
    days: int,
    dry_run: bool,
) -> None:
    print(f"  · chart data: {spec['key']} ({resident_id})")
    seed_bed_state(client, spec, resident_id, room_registry, dry_run=dry_run)
    seed_alarm(client, spec, resident_id, dry_run=dry_run)
    seed_wellbeing(
        client, spec, resident_id, days=spec.get("wellbeing_days", days), dry_run=dry_run
    )
    seed_history(client, spec, resident_id, dry_run=dry_run)


# ── Verify ──────────────────────────────────────────────────────


def verify_resident(client: HubClient, resident_id: str, name: str, days: int) -> None:
    day_list = wellbeing.date_range(days)
    from_d, to_d = day_list[0].isoformat(), day_list[-1].isoformat()
    chart = client.get(f"/api/v1/views/resident-chart/{resident_id}")
    sleep = client.get(
        f"/api/v1/views/resident-chart/{resident_id}/sleep?from={from_d}&to={to_d}"
    )
    falls = client.get(f"/api/v1/views/resident-chart/{resident_id}/falls")
    care = client.get(
        f"/api/v1/views/resident-chart/{resident_id}/care?from={from_d}&to={to_d}"
    )
    n_sleep = len((sleep or {}).get("summaries", []))
    care_proj = care or {}
    n_care = len(care_proj.get("summaries", []))
    care_avg = care_proj.get("avgMinutesPerDay")
    loc = (chart or {}).get("location")
    state = (chart or {}).get("currentState", {}) or {}
    streak = (falls or {}).get("streakDays")
    admission = (chart or {}).get("admissionDate")
    observed_from = (care_proj or {}).get("observedFrom")
    print(
        f"  ✓ {name}: loc={loc} state={state.get('state')} admission={admission} "
        f"observedFrom={observed_from} sleep_days={n_sleep} care_days={n_care} "
        f"care_avg={care_avg} streak={streak}"
    )


# ── CLI ───────────────────────────────────────────────────────────


def filter_residents(keys: list[str] | None) -> list[dict]:
    all_specs = scenario.RESIDENTS + [
        {**r, "unassigned": True} for r in scenario.RESIDENTS_UNASSIGNED
    ]
    if not keys:
        return scenario.RESIDENTS
    key_set = {k.strip() for k in keys}
    return [s for s in all_specs if s["key"] in key_set]


def main() -> int:
    parser = argparse.ArgumentParser(description="Seed demo data for mana-hub panel (via API)")
    parser.add_argument("--base-url", default="http://localhost:8080", help="mana-hub base URL")
    parser.add_argument(
        "--chart-only",
        action="store_true",
        help="No crear facility/residentes; usar ids existentes (p.ej. seed Kotlin con id=lucia)",
    )
    parser.add_argument(
        "--residents",
        help="Comma-separated resident keys (default: todos los asignados del escenario)",
    )
    parser.add_argument("--days", type=int, default=scenario.WELLBEING_DAYS, help="Días de summaries")
    parser.add_argument("--verify", action="store_true", help="Verificar proyecciones al final")
    parser.add_argument("--dry-run", action="store_true", help="Mostrar acciones sin ejecutar")
    args = parser.parse_args()

    client = HubClient(args.base_url)
    resident_keys = args.residents.split(",") if args.residents else None
    specs = filter_residents(resident_keys)

    print(f"mana-hub seed → {args.base_url}")
    if args.dry_run:
        print("(dry-run)")

    if not args.dry_run and not client.alive():
        print("✗ No se pudo conectar al hub. ¿Está corriendo en", args.base_url, "?")
        return 1

    room_registry: dict = {}

    if args.chart_only:
        print("\n[1/2] Chart-only — residentes existentes")
        residents = list_residents(client)
        resolved: list[tuple[dict, str]] = []
        for spec in specs:
            rid = resolve_resident_id(residents, spec["key"])
            if not rid:
                print(f"  ✗ resident not found: {spec['key']}")
                continue
            resolved.append((spec, rid))
            # Construir registry mínimo desde location si hace falta bed state
            loc = next((r.get("location") for r in residents if r["id"] == rid), None)
            if loc and loc.get("bedId"):
                wing = loc.get("wingName") or spec.get("wing", "")
                room = loc.get("roomNumber") or spec.get("room", "")
                label = loc.get("bedLabel") or spec.get("bed_label", "Cama 1")
                room_registry.setdefault((wing, room), {"beds": {}})["beds"][label] = {
                    "bed_id": loc["bedId"],
                    "monitor_key": f"seed-{spec['key']}",
                }
            elif "room" in spec:
                # Fallback: intentar tree de facility demo
                fac = find_facility_by_name(client, scenario.FACILITY["name"])
                if fac:
                    tree = client.get(f"/api/v1/facilities/{fac['id']}/tree")
                    room_registry = build_room_registry(tree)
        print(f"  ✓ resolved {len(resolved)} residents")
    else:
        print("\n[1/3] Facility + layout")
        _, room_registry = ensure_facility(client, args.dry_run)

        print("\n[2/3] Residents + assignments")
        for spec in specs:
            assign = "room" in spec and not spec.get("unassigned")
            ensure_resident(client, spec, dry_run=args.dry_run, assign=assign, room_registry=room_registry)

        residents = [] if args.dry_run else list_residents(client)
        resolved = []
        for spec in specs:
            if "room" not in spec:
                continue
            rid = resolve_resident_id(residents, spec["key"]) if residents else f"dry-{spec['key']}"
            if rid:
                resolved.append((spec, rid))

    print(f"\n[{'2' if args.chart_only else '3'}/{'2' if args.chart_only else '3'}] Chart projections")
    for spec, rid in resolved:
        seed_resident_chart(
            client, spec, rid, room_registry, days=args.days, dry_run=args.dry_run
        )

    if args.verify and not args.dry_run:
        print("\n[verify] Proyecciones resident-chart")
        for spec, rid in resolved:
            verify_resident(client, rid, spec["full_name"], args.days)

    print("\n✓ Seed completo")
    print("  Editá scripts/seed/scenarios/demo_panel.py para cambiar datos")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
