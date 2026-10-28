#!/usr/bin/env bash
# Builds the image context from the release artifacts — never from the working tree.
# Usage: image-context.sh <app.jar> <webapp.tar.gz> [<out-dir>]   (default build/image-context)
# The context is exactly the files listed in ALLOWED. Build metadata the image labels itself with
# (here: a 64-hex fingerprint resource in the jar) is printed on stdout — the only thing printed
# there — for `--build-arg`; notes go to stderr. CI and a person building by hand run the same checks.
set -euo pipefail

die() { echo "image-context: $*" >&2; exit 1; }

[[ $# -ge 2 && $# -le 3 ]] || die "usage: image-context.sh <app.jar> <webapp.tar.gz> [<out-dir>]"
jar=$1
web=$2
out=${3:-build/image-context}
src="$(cd "$(dirname "${BASH_SOURCE[0]}")/../image" && pwd)"   # TODO: entrypoint + log config live here
META_RESOURCE=build-meta.txt                                     # TODO: e.g. schema-fingerprint.txt
ENGINE_PACKAGE='io/ktor/server/engine/*'                          # TODO: a class only the fat jar has

[[ -f $jar ]] || die "no such jar: $jar"
[[ -f $web ]] || die "no such archive: $web"
command -v unzip >/dev/null || die "unzip is required"

# A jar built on a laptop can carry the developer's secrets (application-local*.yaml).
local_cfg=$(unzip -Z1 "$jar" 2>/dev/null | grep -E '(^|/)application-local[^/]*$' || true)
[[ -z $local_cfg ]] || die "$jar carries local configuration, refusing to build an image: $local_cfg"

# A thin jar has no dependencies: the image would build and never start.
unzip -Z1 "$jar" "$ENGINE_PACKAGE" >/dev/null 2>&1 ||
  die "$jar has no dependencies inside — a thin jar, not the shadow jar"

meta=$(unzip -p "$jar" "$META_RESOURCE" 2>/dev/null | tr -d '[:space:]' || true)
[[ $meta =~ ^[0-9a-f]{64}$ ]] || die "$jar has no valid $META_RESOURCE (a release jar has one)"

# Listing into a variable: `grep -q` closing the pipe early would fail `tar` under pipefail.
listing=$(tar -tzf "$web" 2>/dev/null) || die "$web is not a gzip tar archive"
grep -qE '(^|/)index\.html$' <<<"$listing" || die "$web has no index.html — is it the web distribution?"

mkdir -p "$out"
for f in "$out"/* "$out"/.[!.]*; do
  [[ -e $f ]] || continue
  case "$(basename "$f")" in
    app.jar | webapp.tar.gz | entrypoint.sh | logback-container.xml) ;;   # ALLOWED
    *) die "$out holds $(basename "$f"), which is not part of an image context — use an empty directory" ;;
  esac
done
cp "$jar" "$out/app.jar"
cp "$web" "$out/webapp.tar.gz"
cp "$src/entrypoint.sh" "$src/logback-container.xml" "$out/"
chmod 755 "$out/entrypoint.sh"

echo "image-context: $out ready (meta $meta)" >&2
echo "$meta"
