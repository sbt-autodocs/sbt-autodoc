# sbt-autodoc

sbt plugin that reads shared config from your **documentation repo**, diffs your service’s git changes, and writes **Markdown** under `target/autodoc/`. Optional **AI elaboration** (e.g. Claude Code) can polish that output and, with Docusaurus mode, open a branch in the docs repo for a PR.

**additional integration docs:** [docs/build-settings.md](docs/build-settings.md) (every sbt key) · [docs/config-json.md](docs/config-json.md) (`autodoc/config.json` schema).

---

## Publish locally

From this repository:

```bash
cd sbt-autodoc
sbt publishLocal
```

That installs **`autodoc` % `sbt-autodoc` % `0.3.0-SNAPSHOT`** into your local Ivy cache (`~/.ivy2/local/…`).

---

## Use it in `ad-service`

**1. add to `project/plugins.sbt`**

```scala
addSbtPlugin("autodoc" % "sbt-autodoc" % "0.3.0-SNAPSHOT")
```

**2. configure in `build.sbt`** :

```scala
import autodoc.SbtKeys._

autoDocDocumentationRepoUrl := Some("git@github.com:<org>/ad-service-documentation.git")
autoDocDocumentationRef := "main"
autoDocDocumentationConfigPath := "autodoc/config.json"
```

It's a good idea to clone your documentation repo locally in the same directory as your other service repositories. sbt-autodoc will assume, by default, you follow this practice.

If the two repositories cannot live in the same directory (i.e. ~/projects/ad-service and ~/projects/ad-service-documentation), you may override this via:

```scala
autoDocLocalDocumentationRoot := Some(file("/absolute/path/to/ad-service-documentation"))
```

For a full list of build settings and defaults, see **[docs/build-settings.md](docs/build-settings.md)**.

**3. Test Run**

```bash
# simple difference doc without agent elaboration
sbt autoDoc

# agent elaborated documentation (must have claude-cli or cursor-cli):
sbt autoDocElaborate
```

Generated file: **`target/autodoc/autodoc.md`** (build root by default).

**4. Syncing Documentation repo** We highly recommend using `docusaurus`, checkout [Docusaurus site + ADRs](#docusaurus-site--adrs) if you are starting fresh with documentation. 

If you just want a flat repository of markdown files that works too! 

Regardless of your strategy, add the following `autodoc/config.json` with your service id and `pathPrefixes` so the plugin knows which git paths belong to which service. Minimal example:

```json
{
  "version": 1,
  "services": [
    { "id": "ad-service", "title": "Ad Service", "pathPrefixes": ["."] }
  ],
  "defaults": {
    "markdownTemplate": "default.md.tpl",
    "templateRoot": "templates",
    "outputFileName": "autodoc.md"
  }
}
```

Put **`templates/default.md.tpl`** (and any other templates) in that same documentation repository.

---

## Claude Code: generate + elaborate only

Goal: **`autoDoc`** writes raw markdown, then **`autoDocElaborate`** runs **Claude** and writes **`target/autodoc/autodoc-elaborated.md`**.

| Setting | Value |
| --- | --- |
| `autoDocElaborationProvider` | `"claude-code"` |
| `autoDocElaborationMode` | `"execute"` to run Claude; `"handoff"` only writes the prompt file |
| `autoDocElaborationClaudeCodeExecutable` | `"claude"` unless the CLI is elsewhere on your `PATH` |

**Command:**

```bash
sbt autoDoc autoDocElaborate
```

**Useful extras**

- **`autoDocElaborationMermaidDiagrams := "include"`** — if the docs repo has **`.mmd`** files, always add “edit these diagrams in place” to the prompt (good when **`ask`** would fail in CI or non-interactive sbt).
- **`autoDocElaborationServiceDocs := "include"`** — same for existing **`.md`** / pages that mention your service id.
- **`autoDocElaborationCommand := None`** — use the built-in Claude invocation; only set a custom command if you know you need it.

Claude must be installed and on the **`PATH` of the process that starts sbt** (IDE launches sometimes miss this).

---

## Docusaurus site + ADRs

**1. Create the site** (official guide): [Docusaurus installation](https://docusaurus.io/docs/installation). Commit that repo as your **`ad-service-documentation`** (or equivalent).

**2. In the docs repo `autodoc/config.json`**, mark Docusaurus and (for ADRs) date + prefix:

```json
{
  "version": 1,
  "services": [
    { "id": "ad-service", "title": "Ad Service", "pathPrefixes": ["."] }
  ],
  "defaults": {
    "markdownTemplate": "default.md.tpl",
    "templateRoot": "templates",
    "outputFileName": "autodoc.md",
    "documentationRepoKind": "docusaurus",
    "docusaurus": {
      "contentPath": "docs",
      "outputFilePrefix": "adr-",
      "outputFileDatePrefix": true
    }
  }
}
```

- **`contentPath`** — folder under the docs repo root where pages live (match your Docusaurus **`docs`** or custom folder).
- **`outputFileDatePrefix`: true** — filenames like **`2026-03-31-adr-ad-service.md`** (date is UTC).
- Adjust **`outputFilePrefix`** / **`contentPath`** to match how you organize ADRs and **`sidebars`**.

**3. In `ad-service` `build.sbt`**, turn on branch workflow:

```scala
autoDocDocumentationRepoKind := Some("docusaurus")
autoDocDocumentationOutputMode := "documentationBranch"
```

With defaults, **`autoDoc`** still writes under **`target/`**; **`autoDocElaborate`** in **`execute`** mode updates the **documentation checkout** on a new branch (elaborated markdown → **`{serviceId}.md`** under that content path). Push that branch from the docs repo and open a PR.

**Sbt overrides** (optional, same idea as JSON): **`autoDocDocusaurusContentPath`**, **`autoDocDocusaurusOutputFilePrefix`**, **`autoDocDocusaurusOutputFileDatePrefix`**.

---

## Quick fixes

| Symptom | What to check |
| --- | --- |
| Exception about `autodoc/config.json` | File missing in the docs repo, or wrong **`autoDocDocumentationConfigPath`**. |
| `no service mapping for project path …` | Add **`pathPrefixes`** (often **`["."]`**) for a single-repo service, or set **`autoDocServiceId`**. |
| Keys like **`autoDocElaborationProvider`** not found | **`publishLocal`** this plugin, **`reload`**, and use version **`0.3.0-SNAPSHOT`**. **`import autodoc.SbtKeys._`** at the top of **`build.sbt`**. |

---

## Development

```bash
sbt compile
```

## License

See [LICENSE](LICENSE).
