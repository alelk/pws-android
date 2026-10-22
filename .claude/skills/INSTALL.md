# Install the Skills Catalogue

These skills follow the **Anthropic Skills format**: a directory of skill packages, each
with a `SKILL.md` whose YAML frontmatter declares `name:` and `description:`. When the
directory is installed in the right location, Claude scans the descriptions and loads
matching skills on demand.

There are two install modes:

| Mode              | Path                        | Scope                                            |
|-------------------|-----------------------------|--------------------------------------------------|
| **User-level**    | `~/.claude/skills/`         | Available in every Claude session, every project |
| **Project-level** | `<project>/.claude/skills/` | Available only inside that project               |

You can pick one or combine them. This catalogue is also designed to be used as a
**template repository** — copied into new projects when they're created.

---

## Option A — User-level install (symlink)

Recommended: one source of truth, automatically up-to-date.

```bash
mkdir -p ~/.claude/skills

# Symlink each skill directory into ~/.claude/skills/
for skill in /Users/alexelkin/Projects/software-development/engineering-ai-skills/docs/ai/skills/*/; do
    name="$(basename "$skill")"
    ln -sfn "$skill" "$HOME/.claude/skills/$name"
done
```

Verify:

```bash
ls -la ~/.claude/skills/
# kotlin-project-layout -> /Users/.../docs/ai/skills/kotlin-project-layout
# ktor-backend          -> /Users/.../docs/ai/skills/ktor-backend
# ...
```

When you update this catalogue, the changes take effect immediately in new Claude
sessions — no further action required.

---

## Option B — User-level install (copy)

Use this when symlinks aren't appropriate (e.g. you want to pin a snapshot).

```bash
mkdir -p ~/.claude/skills
cp -R /Users/alexelkin/Projects/software-development/engineering-ai-skills/docs/ai/skills/*/ ~/.claude/skills/
```

Re-run after updating the source to refresh.

---

## Option C — Project-level install

Pin specific skills to a single project. Useful when only a subset of skills is relevant,
or you want a different version per repo.

```bash
cd <your-project>
mkdir -p .claude/skills

# Copy or symlink only the skills you want
ln -sfn /Users/alexelkin/Projects/software-development/engineering-ai-skills/docs/ai/skills/kotlin-project-layout       .claude/skills/kotlin-project-layout
ln -sfn /Users/alexelkin/Projects/software-development/engineering-ai-skills/docs/ai/skills/ktor-backend                .claude/skills/ktor-backend
ln -sfn /Users/alexelkin/Projects/software-development/engineering-ai-skills/docs/ai/skills/exposed-postgres            .claude/skills/exposed-postgres
ln -sfn /Users/alexelkin/Projects/software-development/engineering-ai-skills/docs/ai/skills/ktor-api-contract           .claude/skills/ktor-api-contract
```

Either commit `.claude/skills/` (the project pins a snapshot) or git-ignore it (the registry is the
source of truth and each developer installs it). If ignored, say so in `AGENTS.md` so an agent that
wants to "fix a skill" edits the registry, not the local copy.

---

## Option D — Template repository (new project bootstrap)

This catalogue is itself a Kotlin engineering template. To bootstrap a new project:

```bash
# 1. Create the project
mkdir my-new-project && cd my-new-project
git init

# 2. Apply skill templates
cp -R /Users/alexelkin/Projects/software-development/engineering-ai-skills/docs/ai/skills .claude/skills

# 3. Apply foundation templates immediately
mkdir -p gradle
cp .claude/skills/kotlin-project-layout/templates/shared/libs.versions.toml     gradle/libs.versions.toml
cp .claude/skills/kotlin-project-layout/templates/shared/gradle.properties      gradle.properties
cp .claude/skills/kotlin-project-layout/templates/shared/settings.gradle.kts    settings.gradle.kts
cp .claude/skills/kotlin-project-layout/templates/shared/build.gradle.kts.root  build.gradle.kts
cp .claude/skills/kotlin-project-layout/templates/shared/detekt.yml             detekt.yml
cp -R .claude/skills/kotlin-project-layout/templates/convention-plugins         convention-plugins
echo "0.1.0" > app.version

# 4. Process files for agents
cp .claude/skills/doc-first-agentic-workflow/templates/AGENTS.md AGENTS.md
cp .claude/skills/doc-first-agentic-workflow/templates/CLAUDE.md CLAUDE.md

# 5. From here, follow each skill's USAGE.md
```

---

## Verifying discovery

Once installed, start a Claude Code session in any directory and ask:

> "What skills do you have available for Kotlin / Ktor / Compose?"

Claude should list the catalogue skills. If a specific skill isn't being picked up,
check:

1. **Path:** `~/.claude/skills/<skill-name>/SKILL.md` exists (or `<project>/.claude/skills/<skill-name>/SKILL.md`).
2. **Frontmatter:** the file starts with `---\nname: …\ndescription: …\n---`.
3. **Name matches directory:** `name:` field equals the directory name.

---

## Updating

For the symlink install, no action — changes are live immediately.

For the copy install, re-run the relevant `cp -R` command after pulling the catalogue.

For project-level installs, decide per repo whether to follow the catalogue version or
pin a snapshot.

---

## Uninstall

```bash
rm -rf ~/.claude/skills/<skill-name>      # remove one
rm -rf ~/.claude/skills/                  # remove all
```

The source catalogue under `docs/ai/skills/` is untouched.
