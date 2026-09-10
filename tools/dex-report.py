#!/usr/bin/env python3
"""
DEX-отчёт по APK/AAB — метрики, которыми Google Play считает «App optimization».

Читает classes*.dex прямо из архива (или из распакованной папки) и печатает:
  • суммарный несжатый размер DEX  (Play: "Total uncompressed DEX size")
  • число классов и методов
  • разбивку по секциям DEX (код / строки / аннотации / …)
  • долю обфусцированных классов   (Play: "Obfuscation percentage", оценка)
  • топ пакетов по объёму кода     — что именно занимает место

Зависимостей нет — только стандартная библиотека.

Использование:
    python3 tools/dex-report.py output/compose/pws-app-release-3.7.0-ru.apk
    python3 tools/dex-report.py app-compose/build/outputs/apk/ru/release/app-compose-ru-release.apk
    python3 tools/dex-report.py --diff before.apk after.apk
"""

import argparse
import collections
import struct
import sys
import zipfile

SECTION_NAMES = {
    0x0000: "header", 0x0001: "string_id", 0x0002: "type_id", 0x0003: "proto_id",
    0x0004: "field_id", 0x0005: "method_id", 0x0006: "class_def", 0x0007: "call_site_id",
    0x0008: "method_handle", 0x1000: "map_list", 0x1001: "type_list",
    0x1002: "annotation_set_ref_list", 0x1003: "annotation_set", 0x2000: "class_data",
    0x2001: "code", 0x2002: "string_data", 0x2003: "debug_info", 0x2004: "annotation",
    0x2005: "encoded_array", 0x2006: "annotations_directory", 0xF000: "hiddenapi",
}


def u4(b, o):
    return struct.unpack_from("<I", b, o)[0]


def uleb(b, o):
    result = shift = 0
    while True:
        byte = b[o]
        o += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, o
        shift += 7


class Dex:
    """Минимальный парсер DEX: имена классов, размеры кода, карта секций."""

    def __init__(self, data: bytes):
        self.b = data
        self.string_ids_off = u4(data, 0x3C)
        self.type_ids_off = u4(data, 0x44)
        self.class_defs_size = u4(data, 0x60)
        self.class_defs_off = u4(data, 0x64)
        self.map_off = u4(data, 0x34)

    def string(self, idx: int) -> str:
        off = u4(self.b, self.string_ids_off + 4 * idx)
        _, off = uleb(self.b, off)
        end = self.b.index(b"\x00", off)
        return self.b[off:end].decode("utf-8", "replace")

    def type_name(self, idx: int) -> str:
        return self.string(u4(self.b, self.type_ids_off + 4 * idx))

    def sections(self):
        """[(имя_секции, байт, элементов)] — по map_list."""
        n = u4(self.b, self.map_off)
        items = []
        for i in range(n):
            o = self.map_off + 4 + 12 * i
            kind = struct.unpack_from("<H", self.b, o)[0]
            items.append((u4(self.b, o + 8), kind, u4(self.b, o + 4)))
        items.sort()
        out = []
        for i, (off, kind, count) in enumerate(items):
            end = items[i + 1][0] if i + 1 < len(items) else len(self.b)
            out.append((SECTION_NAMES.get(kind, hex(kind)), end - off, count))
        return out

    def classes(self):
        """[(имя_класса, байт_кода_и_class_data, методов)]"""
        b = self.b
        for i in range(self.class_defs_size):
            off = self.class_defs_off + 32 * i
            name = self.type_name(u4(b, off)).strip("L;").replace("/", ".")
            data_off = u4(b, off + 24)
            size, methods = 32, 0
            if data_off:
                o = data_off
                static_f, o = uleb(b, o)
                inst_f, o = uleb(b, o)
                direct_m, o = uleb(b, o)
                virtual_m, o = uleb(b, o)
                for _ in range(static_f + inst_f):
                    _, o = uleb(b, o)
                    _, o = uleb(b, o)
                for _ in range(direct_m + virtual_m):
                    _, o = uleb(b, o)
                    _, o = uleb(b, o)
                    code_off, o = uleb(b, o)
                    methods += 1
                    if code_off:
                        size += 16 + u4(b, code_off + 12) * 2
                size += o - data_off
            yield name, size, methods


def load_dex_blobs(path: str):
    """Возвращает [(имя, bytes)] для всех classes*.dex внутри apk/aab/папки/файла."""
    if path.endswith((".apk", ".aab", ".zip")):
        with zipfile.ZipFile(path) as z:
            names = [n for n in z.namelist()
                     if n.endswith(".dex") and ("/dex/" in n or "/" not in n)]
            if not names:
                names = [n for n in z.namelist() if n.endswith(".dex")]
            return [(n, z.read(n)) for n in sorted(names)]
    if path.endswith(".dex"):
        with open(path, "rb") as f:
            return [(path, f.read())]
    import os
    blobs = []
    for root, _, files in os.walk(path):
        for f in sorted(files):
            if f.endswith(".dex"):
                with open(os.path.join(root, f), "rb") as fh:
                    blobs.append((os.path.join(root, f), fh.read()))
    return blobs


def is_obfuscated(fqcn: str) -> bool:
    """Эвристика «класс переименован R8»: короткое имя класса или короткие сегменты пакета."""
    parts = fqcn.split(".")
    simple = parts[-1].split("$")[0]
    if len(simple) <= 3:
        return True
    pkg = parts[:-1]
    return bool(pkg) and all(len(seg) <= 2 for seg in pkg)


def analyze(path: str):
    blobs = load_dex_blobs(path)
    if not blobs:
        sys.exit(f"[ERROR] в {path} не найдено ни одного classes*.dex")

    total_bytes = sum(len(b) for _, b in blobs)
    sections = collections.Counter()
    section_counts = collections.Counter()
    pkg_size = collections.Counter()
    pkg_classes = collections.Counter()
    classes = methods = obfuscated = 0

    for _, blob in blobs:
        dex = Dex(blob)
        for name, size, count in dex.sections():
            sections[name] += size
            section_counts[name] += count
        for name, size, nmethods in dex.classes():
            classes += 1
            methods += nmethods
            if is_obfuscated(name):
                obfuscated += 1
            parts = name.split(".")
            pkg = ".".join(parts[:3]) if len(parts) >= 3 else name
            pkg_size[pkg] += size
            pkg_classes[pkg] += 1

    return {
        "path": path, "dex_files": len(blobs), "dex_bytes": total_bytes,
        "classes": classes, "methods": methods, "obfuscated": obfuscated,
        "sections": sections, "section_counts": section_counts,
        "pkg_size": pkg_size, "pkg_classes": pkg_classes,
    }


def print_report(r, top=25):
    print(f"\n=== {r['path']} ===")
    print(f"DEX-файлов:            {r['dex_files']}")
    print(f"Несжатый размер DEX:   {r['dex_bytes'] / 1e6:.2f} MB   "
          f"(Play: «Total uncompressed DEX size»)")
    print(f"Классов:               {r['classes']:,}")
    print(f"Методов:               {r['methods']:,}")
    pct = 100 * r["obfuscated"] / r["classes"] if r["classes"] else 0
    print(f"Обфусцировано классов: {r['obfuscated']:,} / {r['classes']:,} = {pct:.0f}%   "
          f"(оценка «Obfuscation percentage»)")

    print("\n-- секции DEX --")
    for name, size in sorted(r["sections"].items(), key=lambda kv: -kv[1])[:10]:
        print(f"  {name:24}{size / 1e6:>8.2f} MB   {r['section_counts'][name]:>9,} элем.")

    print(f"\n-- топ-{top} пакетов по объёму кода --")
    print(f"  {'пакет':52}{'классов':>9}{'код, MB':>10}")
    for pkg, size in r["pkg_size"].most_common(top):
        print(f"  {pkg:52}{r['pkg_classes'][pkg]:>9,}{size / 1e6:>10.2f}")
    print()


def print_diff(a, b):
    print_report(a, top=12)
    print_report(b, top=12)

    def line(label, x, y, unit="", scale=1.0, fmt="{:,.0f}"):
        dx = y - x
        pct = (100.0 * dx / x) if x else 0.0
        sx, sy, sd = (fmt.format(v / scale) for v in (x, y, dx))
        print(f"  {label:26}{sx:>12}{unit} → {sy:>12}{unit}   {sd:>12}{unit}  ({pct:+.1f}%)")

    print("=== ДЕЛЬТА (было → стало) ===")
    line("Несжатый DEX", a["dex_bytes"], b["dex_bytes"], " MB", 1e6, "{:,.2f}")
    line("Классов", a["classes"], b["classes"])
    line("Методов", a["methods"], b["methods"])
    pa = 100 * a["obfuscated"] / max(a["classes"], 1)
    pb = 100 * b["obfuscated"] / max(b["classes"], 1)
    print(f"  {'Обфускация':26}{pa:>12.0f}%  → {pb:>12.0f}%")
    print()


def main():
    ap = argparse.ArgumentParser(description="DEX-отчёт по APK/AAB (метрики Play App optimization)")
    ap.add_argument("target", nargs="?", help="путь к .apk / .aab / .dex / папке")
    ap.add_argument("--diff", nargs=2, metavar=("BEFORE", "AFTER"),
                    help="сравнить две сборки")
    ap.add_argument("--top", type=int, default=25, help="сколько пакетов показать (по умолчанию 25)")
    ap.add_argument("--max-dex-mb", type=float, metavar="N",
                    help="гейт для CI: выйти с кодом 1, если несжатый DEX больше N мегабайт")
    args = ap.parse_args()

    if args.diff:
        print_diff(analyze(args.diff[0]), analyze(args.diff[1]))
        return
    if not args.target:
        ap.error("укажите путь к APK/AAB или --diff BEFORE AFTER")

    report = analyze(args.target)
    print_report(report, top=args.top)

    if args.max_dex_mb is not None:
        actual = report["dex_bytes"] / 1e6
        if actual > args.max_dex_mb:
            print(f"::error::DEX вырос до {actual:.2f} MB при пороге {args.max_dex_mb:.2f} MB. "
                  f"Скорее всего в proguard-rules.pro появилось широкое keep-правило — "
                  f"см. docs/ai/plans/2026-09-10_app-optimization-r8_plan.md")
            sys.exit(1)
        print(f"OK: DEX {actual:.2f} MB ≤ порога {args.max_dex_mb:.2f} MB\n")


if __name__ == "__main__":
    main()
