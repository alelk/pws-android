# Single-workflow release + image (`docker-app`, preferred variant)

The two-stage form ([`docker-publish-two-stage.md`](docker-publish-two-stage.md)) leaves the image
to a second workflow. The obvious way to automate that second workflow — `on: release` or
`on: workflow_run` — **does not fire**: events created with the repository's `GITHUB_TOKEN` do not
trigger other workflows (GitHub's recursion guard). The release appears, the image never does, and
nothing is red.

The same job has a second trap: if the release is made **first** and the image after it, a failed
image build leaves the tag and the GitHub Release behind. The re-run finds "nothing to release"
(the tag exists, no new commits), says `released=false`, skips the image — and the image never
appears. **Publish the image first, release second**: a failed image leaves no tag, so the re-run
computes the same version and tries again.

*Mine:* "released" without an image (either way round); or a hand-run publish that rebuilds from
the working tree instead of the released bytes.

## Shape

```
ci.yml
├── build    (every push/PR)  ./gradlew build, test report, stop daemons     permissions: contents read, checks write
└── release  (push to main/next, needs build)                               permissions: contents/issues/PRs/packages write
    1. npm ci the release tools from a lockfile (--ignore-scripts)
    2. semantic-release --dry-run          → writes the next version into app.version
    3. assemble the release artifacts      → shadowJar + verifyReleaseJar + web dist tarball
    3a. release due?                       → `app.version` on disk ≠ committed → step output `due`, `version`
    4. stop the Gradle/Kotlin daemons      (the image build needs the memory)
    5. if due: image context from the artifacts → buildx (amd64+arm64) → push :<version>
    6. semantic-release                    → tag, GitHub Release with the artifacts, CHANGELOG commit
       (fails the job if it released a version other than the one the image was built for)
image.yml (workflow_dispatch, input release_tag)
    rebuilds the image of an EXISTING release from its attached assets (base-image refresh)
```

Template: [`templates/docker-app/ci-single-workflow.yml`](../templates/docker-app/ci-single-workflow.yml),
rebuild: [`templates/docker-app/rebuild-image.yml`](../templates/docker-app/rebuild-image.yml).

## Release due, then image, then release

The dry-run (step 2) runs `verifyReleaseCmd` → `prepare.sh`, which writes the **next** version into
`app.version`; with nothing to release the file keeps the committed value. So *"a release is due"*
is `app.version` on disk ≠ `git show HEAD:app.version` — known before anything is published, and
the artifacts (step 3) already carry that version.

```bash
before="$(git show HEAD:app.version)"     # the committed version, not the working copy
after="$(cat app.version)"                # the dry-run wrote the next one here
if [ "$after" != "$before" ]; then echo "due=true"; echo "version=${after}"; else echo "due=false"; fi
```

Every image step is gated on `due`; the image is tagged with that version; then `semantic-release`
runs. Tag existence alone is wrong as a gate: on a push without a release `app.version` still names
the previous, already published tag, and republishing it **overwrites an image tag in use**.

Two guards close the holes of "image before release":

- the real `semantic-release` step compares the version it released with the one the image was
  built for and **fails the job** (`::error::` naming the `image.yml` run to do) if they differ — it
  also catches a dry-run that failed silently (`|| true`) and so published nothing;
- the failure that remains — the release dying *after* the tag was pushed (e.g. while attaching
  assets) — has its image already; finish the release by hand. An image without a release is
  harmless: nothing references it, and the re-run overwrites it with the same bytes.

## The image context is the artifacts, never the working tree

The runtime Dockerfile compiles nothing. A script assembles a context of **exactly** the files the
image needs (the shadow jar, the web tarball, the entrypoint, the container log config) and refuses
anything else ([`templates/docker-app/image-context.sh`](../templates/docker-app/image-context.sh)):

- refuses a jar that carries local configuration (`application-local*`);
- refuses a **thin** jar (no server engine classes inside: the image would build and never start);
- reads build metadata out of the jar (e.g. a schema fingerprint resource) and prints it on stdout —
  the only stdout line — for `--build-arg`; notes go to stderr;
- refuses a web archive without `index.html`;
- refuses a non-empty output directory holding foreign files.

The same script serves CI and a person building by hand, so the checks hold for both. Artifact
checks in Gradle → [`release-artifact-verification.md`](release-artifact-verification.md).

## Base images pinned by digest, and the refresh

Pin both stages by **index digest** (`image:tag@sha256:…` of the multi-arch index, so one pin serves
amd64 and arm64). Write the refresh recipe in the Dockerfile header:

```dockerfile
# Refresh: `docker buildx imagetools inspect <name>:<tag>` → copy the index digest here,
# then run image.yml for the current release (no new release needed).
```

Without a rebuild workflow, a base-image security fix waits for the next feature release.

## What "release" means

A release is **built, versioned, image published** — it deploys nothing. Who deploys and how is the
delivery runbook's ([`single-host-stand-deploy`](../../single-host-stand-deploy/SKILL.md)). If the
server sends its version on every response (`X-App-Version`) and the client compares it with its
own, a stale client can ask the user to reload instead of failing on a changed contract; whether the
schema may change inside a release is [`pre-deploy-schema-policy`](../../pre-deploy-schema-policy/SKILL.md)'s
question.

## Variants / open decisions

- Image tags: version only, no `latest` (a deploy names a version; `latest` makes rollback
  ambiguous) — or `latest` as a convenience for previews. Decide once.
- Self-hosted runner vs GitHub-hosted: the release job builds a multi-arch image under QEMU; on a
  2-vCPU hosted runner that is slow but works. Keep the hosted `runs-on` as a comment next to the
  self-hosted one, with the line to revert.

## Evidence

family-planner `.github/workflows/ci.yml` (comment "a release made with GITHUB_TOKEN does not
trigger other workflows, so `on: release` would never fire", step 66 K10), `image.yml`,
`deploy/bin/image-context.sh`, `deploy/Dockerfile` header, ADR 0067 (release ≠ deploy,
`X-App-Version`).
