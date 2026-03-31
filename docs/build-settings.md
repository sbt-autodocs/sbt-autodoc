# sbt-autodoc build settings

This page documents every **setting** and **task** exposed by the plugin. Keys live on **`AutoDocPlugin.autoImport`** and **`autodoc.SbtKeys`** (same instances).

```scala
import autodoc.SbtKeys._
// or: import autodoc.AutoDocPlugin.autoImport._
```

Use **`ThisBuild / key`** when you want one value for all projects; otherwise set keys on the project where you run **`autoDoc`** / **`autoDocElaborate`**.

---

## Tasks

### `autoDoc`

- **Type:** `TaskKey[File]`
- **Runs:** Loads config from the documentation repo, runs **`git diff --name-status`**, resolves the service, renders the template, writes **`autoDocOutputFile`**.
- **Skips:** When **`autoDocPerSubproject`** is **`false`**, nested aggregate projects log a skip (run on the **root** project, or enable per-subproject mode).

### `autoDocElaborate`

- **Type:** `TaskKey[Seq[File]]`
- **Runs:** Depends on **`autoDoc`**. If **`autoDocElaborationProvider`** is **`none`**, logs and returns empty. Otherwise builds the elaboration prompt (and optionally runs the provider when **`autoDocElaborationMode`** is **`execute`**). May publish to the documentation branch when that mode is enabled (see **Docusaurus & branch workflow** below).

---

## Documentation repository

These control **where** the plugin finds **`autoDocDocumentationConfigPath`** and templates (under that checkout’s root).

### `autoDocDocumentationRepoUrl`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None`
- **Purpose:** HTTPS or SSH URL of the documentation git repository (e.g. `git@github.com:org/ad-service-documentation.git`).
- **Behavior:** Used when **`autoDocLocalDocumentationRoot`** is unset. Together with **`autoDocDocumentationRepoResolution`**, the plugin either uses a **sibling** directory next to the service project or clones/updates under **`autoDocDocumentationCacheDirectory`**.

### `autoDocLocalDocumentationRoot`

- **Type:** `SettingKey[Option[File]]`
- **Default:** `None`
- **Purpose:** Absolute path to an existing clone of the documentation repo.
- **Behavior:** When set, it **wins** over **`autoDocDocumentationRepoUrl`** (no clone from URL for that resolution path). The plugin still **fetches / checks out / fast-forwards** to **`autoDocDocumentationRef`** when possible.

### `autoDocDocumentationRef`

- **Type:** `SettingKey[String]`
- **Default:** `"main"`
- **Purpose:** Branch or tag to sync in the documentation checkout before reading config and templates.

### `autoDocDocumentationConfigPath`

- **Type:** `SettingKey[String]`
- **Default:** `"autodoc/config.json"`
- **Purpose:** Path **inside the documentation repo root** to the JSON config (POSIX-style, e.g. `config/autodoc.json`).

### `autoDocDocumentationRepoResolution`

- **Type:** `SettingKey[String]`
- **Default:** `"sibling"`
- **Values:**
  - **`sibling`** — Resolve **`../<repo-name>/`** from the URL’s last path segment next to **`LocalRootProject` `baseDirectory`**. If the directory exists as a git repo: fetch/checkout/pull. If missing: shallow clone.
  - **`cache`** — Use only **`autoDocDocumentationCacheDirectory`** (clone or update there).

### `autoDocDocumentationCacheDirectory`

- **Type:** `SettingKey[File]`
- **Default:** `(LocalRootProject / baseDirectory) / target / autodoc / documentation-repo`
- **Purpose:** Clone/update location when **`autoDocDocumentationRepoResolution`** is **`cache`**. Ignored for placement when using **`sibling`** (but the key still exists).

---

## Git diff scope

Autodoc summarizes **which files changed**; these settings define the **`git diff`** range. Diff runs from the **service project’s** `baseDirectory` (subject to **`autoDocPerSubproject`**).

### `autoDocGitDiffScope`

- **Type:** `SettingKey[String]`
- **Default:** `"branch"`
- **Values:**
  - **`branch`** — `git diff <merge-base(autoDocGitBranchBase, HEAD)>...HEAD` (all commits on the current branch vs merge-base with the base ref).
  - **`uncommitted`** — `git diff HEAD` (working tree + index vs `HEAD`).

### `autoDocGitBranchBase`

- **Type:** `SettingKey[String]`
- **Default:** `"origin/main"`
- **Purpose:** Left-hand side of the three-dot range when scope is **`branch`**. Use **`origin/master`** if your default branch is **`master`**.

### `autoDocGitDiffSpec`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None`
- **Purpose:** When **set**, overrides **`autoDocGitDiffScope`** and **`autoDocGitBranchBase`**. The value is passed as the **sole revision expression** to **`git diff --name-status`** (e.g. `Some("origin/develop...HEAD")`).

---

## Service identity and primary output

### `autoDocServiceId`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None`
- **Purpose:** Force the service entry from JSON by **`id`**. Required when path-based inference from **`pathPrefixes`** does not match your layout.

### `autoDocPerSubproject`

- **Type:** `SettingKey[Boolean]`
- **Default:** `false`
- **Purpose:** **`false`** — single logical project: **`autoDoc`** / **`autoDocElaborate`** run on the **build root** only; output and git context use **`LocalRootProject` `baseDirectory`**. **`true`** — each subproject gets its own **`target/autodoc/`** output and diff scoped to that module’s base directory.

### `autoDocOutputFile`

- **Type:** `SettingKey[File]`
- **Default:** `target/autodoc/autodoc.md` under the root or subproject `target/` depending on **`autoDocPerSubproject`**.
- **Purpose:** Raw **generated** markdown from the template (input to elaboration).

---

## Repository kind and Docusaurus

### `autoDocDocumentationRepoKind`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None` (falls back to **`defaults.documentationRepoKind`** in JSON, then **`generic`**).
- **Values:** **`generic`** — templates + JSON only; output stays in the service repo unless you copy files yourself. **`docusaurus`** — enables writing under the configured Docusaurus content path when **`autoDocDocumentationOutputMode`** is **`documentationBranch`**.

### `autoDocDocumentationOutputMode`

- **Type:** `SettingKey[String]`
- **Default:** `"serviceTarget"`
- **Values:**
  - **`serviceTarget`** — Only write **`autoDocOutputFile`** (and elaboration outputs) under the **service** repo.
  - **`documentationBranch`** — Create a branch in the **documentation** checkout, write the Docusaurus markdown file there, then optionally **`git add` / commit** (see git keys below). Requires Docusaurus layout (**`autoDocDocumentationRepoKind`** / JSON **`docusaurus`**).

### `autoDocDocusaurusContentPath`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None` (JSON **`defaults.docusaurus.contentPath`**, else plugin default **`docs`**).
- **Purpose:** Directory **under the documentation repo root** where the generated **`.md`** file is written (POSIX path; backslashes normalized). A lone **`.`** is treated as unset (avoids writing at repo root by mistake).

### `autoDocDocusaurusOutputFilePrefix`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None` (JSON **`defaults.docusaurus.outputFilePrefix`**).
- **Purpose:** Text inserted before the base filename (e.g. **`adr-`** → **`adr-edge-ctrl.md`**). Non-empty sbt value overrides JSON.

### `autoDocDocusaurusOutputFileDatePrefix`

- **Type:** `SettingKey[Option[Boolean]]`
- **Default:** `None` (JSON **`defaults.docusaurus.outputFileDatePrefix`**, else false).
- **Purpose:** **`Some(true)`** — prepend **`yyyy-MM-dd-`** (UTC) for ADR-style names. **`Some(false)`** — never add a date, even if JSON says otherwise. **`None`** — use JSON / default.

**Filename order on the documentation branch:** date (if enabled) → custom prefix → base name from **`outputFileName`** / **`{{serviceId}}`** (see JSON docs).

---

## Documentation branch (git + source)

Used when **`autoDocDocumentationOutputMode`** is **`documentationBranch`**.

### `autoDocDocumentationBranchName`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None` → generated name **`autodoc/<serviceId>-<yyyyMMdd-HHmmss>`** (UTC).

### `autoDocDocumentationBranchMarkdownSource`

- **Type:** `SettingKey[String]`
- **Default:** `"elaborated"`
- **Values:**
  - **`generated`** — **`autoDoc`** writes **template** output to the branch file during **`autoDoc`**.
  - **`elaborated`** — **`autoDoc`** only writes under **`target/`** in the service repo; **`autoDocElaborate`** in **`execute`** mode writes **elaborated** content to the documentation branch (e.g. **`{serviceId}.md`**).

### `autoDocDocumentationCommit`

- **Type:** `SettingKey[Boolean]`
- **Default:** `true`
- **Purpose:** When **`true`**, run **`git add`** and **`git commit`** on the generated path on the new branch. When **`false`**, no commit (you commit locally). Git **`user.name`** / **`user.email`** must be set when committing.

### `autoDocDocumentationCommitMessage`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None` → message like **`autodoc: update <serviceId> documentation`**.

### `autoDocDocumentationGitStage`

- **Type:** `SettingKey[Boolean]`
- **Default:** `false`
- **Purpose:** Only meaningful when **`autoDocDocumentationCommit`** is **`false`**. If **`true`**, run **`git add`** on the **generated documentation file path only** (no commit). Ignored when commit is **`true`** (staging happens as part of commit).

---

## Elaboration: core

### `autoDocElaborationProvider`

- **Type:** `SettingKey[String]`
- **Default:** `"none"`
- **Values:** **`none`** — skip elaboration logic. **`claude-code`** — built-in Claude CLI integration when **`autoDocElaborationCommand`** is unset. **`cursor-cli`** — built-in Cursor **`agent`** integration. Other strings may be normalized (see plugin); **`claude-api`** is reserved for future use.

### `autoDocElaborationMode`

- **Type:** `SettingKey[String]`
- **Default:** `"handoff"`
- **Values:** **`handoff`** — write **`autoDocElaborationPromptFile`** only (no provider subprocess unless you use a custom command). **`execute`** — run **`autoDocElaborationCommand`** if set, otherwise the built-in provider command for **`claude-code`** / **`cursor-cli`**.

### `autoDocElaborationPromptFile`

- **Type:** `SettingKey[File]`
- **Default:** `target/autodoc/elaboration-prompt.md` (root vs subproject per **`autoDocPerSubproject`**).
- **Purpose:** Full prompt written for handoff and execute modes (provider may embed it).

### `autoDocElaborationOutputFile`

- **Type:** `SettingKey[File]`
- **Default:** `target/autodoc/autodoc-elaborated.md`
- **Purpose:** Where the AI is told to write elaborated markdown; also the file read when publishing **`elaborated`** output to the documentation branch.

### `autoDocElaborationAudience`

- **Type:** `SettingKey[String]`
- **Default:** `"engineering"`
- **Purpose:** Injected into the elaboration prompt.

### `autoDocElaborationTone`

- **Type:** `SettingKey[String]`
- **Default:** `"concise"`
- **Purpose:** Injected into the elaboration prompt.

### `autoDocElaborationCustomPrompt`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None`
- **Purpose:** Extra markdown section appended to the generated prompt body.

### `autoDocElaborationCommand`

- **Type:** `SettingKey[Option[String]]`
- **Default:** `None`
- **Purpose:** In **`execute`** mode, if **set**, this shell command is run **instead of** the built-in Claude/Cursor commands. Placeholders: **`{input}`** (generated autodoc path), **`{output}`** (elaborated output path), **`{prompt}`** (prompt file path). Exit code non-zero fails the task.

---

## Elaboration: Claude Code (built-in)

### `autoDocElaborationClaudeCodeExecutable`

- **Type:** `SettingKey[String]`
- **Default:** `"claude"`
- **Purpose:** Executable name or path when **`autoDocElaborationProvider`** is **`claude-code`** and **`autoDocElaborationCommand`** is **`None`**.

### `autoDocElaborationClaudeCodeArgs`

- **Type:** `SettingKey[Seq[String]]`
- **Default:** `Seq.empty`
- **Purpose:** Extra arguments after **`-p <prompt>`** for the default **`claude-code`** invocation (e.g. model flags).

---

## Elaboration: Cursor CLI (built-in)

### `autoDocElaborationCursorCliExecutable`

- **Type:** `SettingKey[String]`
- **Default:** `"agent"`
- **Purpose:** Cursor’s CLI is typically **`agent`**, not **`cursor-cli`**. Use an absolute path if **`PATH`** in the JVM that runs sbt does not include the binary.

### `autoDocElaborationCursorCliArgs`

- **Type:** `SettingKey[Seq[String]]`
- **Default:** `Seq.empty`
- **Purpose:** Extra arguments after the prompt for the default **`cursor-cli`** invocation.

---

## Elaboration: optional prompt sections (documentation repo)

These only affect **what is added to the elaboration prompt** when the documentation checkout is resolved and (for service docs) config loads successfully.

### `autoDocElaborationMermaidDiagrams`

- **Type:** `SettingKey[String]`
- **Default:** `"ask"`
- **Values:** **`include`** — if any **`.mmd`** files exist under the documentation repo, always add a section listing them and instructing in-place edits. **`skip`** — never add. **`ask`** — prompt via sbt **`InteractionService`** (may behave like “no” in non-interactive or CI environments).

### `autoDocElaborationServiceDocs`

- **Type:** `SettingKey[String]`
- **Default:** `"ask"`
- **Purpose:** Same **`include`** / **`skip`** / **`ask`** behavior for **non-`.mmd`** files whose **name** or **text** references the resolved **service id** (e.g. existing **`edge-ctrl.md`**). **`.mmd`** files are only listed under the Mermaid section.

---

## JSON vs sbt

Many behaviors also exist under **`defaults`** in **`autodoc/config.json`** (e.g. **`documentationRepoKind`**, **`docusaurus`**, **`outputFileName`**). **Sbt wins** when you set the corresponding **`Option`** keys to non-empty / non-default overrides, as documented on each key above.

---

## See also

- [README](../README.md) — quick start, local **`publishLocal`**, minimal **`ad-service`** setup.
- [config-json.md](config-json.md) — **`autodoc/config.json`** schema (services, **`pathPrefixes`**, defaults, Docusaurus).
