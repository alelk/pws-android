#!/usr/bin/env python3
"""
Writes the golden `pws-app-preferences.preferences_pb` fixtures of the RuStore fork (plan 2026-09-29,
T-B05) into app-compose/src/testRustore/resources/legacy-entitlement/.

The files are encoded here byte by byte from the androidx DataStore Preferences wire format
(`PreferenceMap { map<string, Value> preferences = 1; }`, unchanged since datastore 1.0 — the fork
used 1.1.7), deliberately NOT with the app's own DataStore code, so the tests check the app against
an independent encoder. Dates are fixed; the tests inject a clock at 2026-09-29.

Usage:
  python3 tools/make-legacy-entitlement-fixtures.py
      → all golden fixtures (fixed dates) into app-compose/src/testRustore/resources/legacy-entitlement/
  python3 tools/make-legacy-entitlement-fixtures.py --single sub_future --out /tmp/x.preferences_pb
      → one fixture for a device test, with dates relative to today (future = +30 days,
        past = -30 days), used by e2e/scripts/rustore-entitlement.sh
"""
import argparse
import datetime
import os
import random
import struct

OUT = os.path.join(os.path.dirname(__file__), "..", "app-compose", "src", "testRustore", "resources", "legacy-entitlement")


def varint(n: int) -> bytes:
    out = bytearray()
    while True:
        b = n & 0x7F
        n >>= 7
        out.append(b | (0x80 if n else 0))
        if not n:
            return bytes(out)


def field(num: int, wire: int, payload: bytes) -> bytes:
    return varint(num << 3 | wire) + payload


def length_delimited(num: int, data: bytes) -> bytes:
    return field(num, 2, varint(len(data)) + data)


def value(v) -> bytes:
    # Value oneof: boolean = 1, float = 2, integer = 3, long = 4, string = 5, string_set = 6, double = 7
    if isinstance(v, bool):
        return field(1, 0, varint(int(v)))
    if isinstance(v, float):
        return field(2, 5, struct.pack("<f", v))
    if isinstance(v, str):
        return length_delimited(5, v.encode("utf-8"))
    raise TypeError(v)


def preference_map(prefs: dict) -> bytes:
    out = b""
    for key, v in prefs.items():
        entry = length_delimited(1, key.encode("utf-8")) + length_delimited(2, value(v))
        out += length_delimited(1, entry)
    return out


# The fork's display settings live in the same file; every fixture carries some, as on a real device.
SETTINGS = {"app-theme": "dark", "song-text-expanded": False, "song-text-size": 18.0}

FIXTURES = {
    "none.preferences_pb": {**SETTINGS},
    "lifetime.preferences_pb": {**SETTINGS, "purchase_full_access": True},
    "lifetime_false_sub.preferences_pb": {**SETTINGS, "purchase_full_access": False, "purchase_subscription_until": "2026-10-15"},
    "sub_future.preferences_pb": {**SETTINGS, "purchase_subscription_until": "2026-10-15"},
    "sub_past.preferences_pb": {**SETTINGS, "purchase_subscription_until": "2026-09-01"},
    "sub_and_full.preferences_pb": {**SETTINGS, "purchase_full_access": True, "purchase_subscription_until": "2026-09-01"},
    "bad_date.preferences_pb": {**SETTINGS, "purchase_subscription_until": "garbage"},
}


def relative_fixtures(today: datetime.date) -> dict:
    future = (today + datetime.timedelta(days=30)).isoformat()
    past = (today - datetime.timedelta(days=30)).isoformat()
    shift = {"2026-10-15": future, "2026-09-01": past}
    return {
        name.removesuffix(".preferences_pb"): {k: shift.get(v, v) if isinstance(v, str) else v for k, v in prefs.items()}
        for name, prefs in FIXTURES.items()
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--single", help="fixture kind, e.g. lifetime / sub_future / sub_past / none / corrupted")
    parser.add_argument("--out", help="output file for --single")
    args = parser.parse_args()
    if args.single:
        with open(args.out, "wb") as f:
            if args.single == "corrupted":
                f.write(bytes([0xFF, 0xFF, 0xFF, 0xFF]) + os.urandom(60))
            else:
                f.write(preference_map(relative_fixtures(datetime.date.today())[args.single]))
        return
    os.makedirs(OUT, exist_ok=True)
    for name, prefs in FIXTURES.items():
        with open(os.path.join(OUT, name), "wb") as f:
            f.write(preference_map(prefs))
    # Not a protobuf at all: DataStore must report corruption, and the app must leave the file alone.
    rnd = random.Random(20260929)
    with open(os.path.join(OUT, "corrupted.preferences_pb"), "wb") as f:
        f.write(bytes([0xFF, 0xFF, 0xFF, 0xFF]) + bytes(rnd.randrange(256) for _ in range(60)))
    print(f"fixtures written to {os.path.normpath(OUT)}")


if __name__ == "__main__":
    main()
