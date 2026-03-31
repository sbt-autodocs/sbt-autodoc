# `autodoc/config.json` reference

The documentation repository must contain a JSON file (default path: **`autodoc/config.json`**, overridable with **`autoDocDocumentationConfigPath`**). The plugin loads it with [circe](https://circe.github.io/circe/); only **`version: 1`** is accepted at runtime.

---

## Top-level object

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| **`version`** | number | yes | Must be **`1`**. Other values fail with an unsupported-version error. |
| **`services`** | array | yes | One or more service definitions (see below). At least one entry must match the sbt project or **`autoDocServiceId`**. |
| **`defaults`** | object | no | Shared template paths, optional Docusaurus defaults, and output naming. |

---

## `services[]` — mapping git paths to a service

Each element describes a **logical service** and which paths in the **service git repository** belong to it.

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| **`id`** | string | yes | Stable identifier (e.g. `ad-service`, `edge-ctrl`). Used in templates as **`{{serviceId}}`**, in Docusaurus filenames, and for **`autoDocServiceId`**. |
| **`title`** | string | no | Human-readable name; defaults to **`id`** in templates as **`{{serviceTitle}}`**. |
| **`pathPrefixes`** | array of string | yes | POSIX-style path prefixes relative to the **service repo** git root. Used to decide which **`services[]`** entry applies to the current sbt project (see **Path matching**). |

### Path matching

Resolution is implemented in **`ConfigResolver`**:

1. **Normal prefixes** — For each service, each prefix is normalized (forward slashes, trailing `/`). The **longest** matching prefix wins when the project’s path under the repo **equals** the prefix without its trailing slash, or **starts with** the prefix as a directory segment (e.g. prefix `ad-service/` matches `ad-service/foo`).
2. **Catch-all** — A prefix of **`""`**, **`.`**, or **`*`** means “this service owns the **entire** repository.” Use this when one git repo is one product with many sbt modules (`core/`, `service/`, …) so every module maps to the same service.
3. **Override** — If **`autoDocServiceId`** is set in sbt, the plugin selects that **`id`** from **`services`** and ignores path-based matching (the **`id`** must exist).

---

## `defaults` — templates and layout

All fields are optional; the plugin supplies fallbacks when unset.

| Field | Type | Description |
| --- | --- | --- |
| **`markdownTemplate`** | string | Filename of the template under **`templateRoot`** (default: **`default.md.tpl`**). |
| **`templateRoot`** | string | Directory under the **documentation repo root** containing templates (default: **`templates`**). |
| **`outputFileName`** | string | Base name for **Docusaurus branch** output (not the primary **`target/autodoc/autodoc.md`** path). **`autodoc.md`** or omission means “use **`<sanitized-service-id>.md`**”. Any other value is used as a template: embed **`{{serviceId}}`** (e.g. **`{{serviceId}}.md`**). Slashes in the template are replaced with **`_`**. |
| **`documentationRepoKind`** | string | **`generic`** (default) — JSON + templates only. **`docusaurus`** — enables writing under a content path when using **`documentationBranch`** mode in sbt. |
| **`docusaurus`** | object | Optional nested object (see next section). If omitted, Docusaurus content path falls back to **`docs`** when resolving branch output (unless overridden in sbt). |

### `defaults.docusaurus`

Present only when you want JSON-driven Docusaurus layout. **If this object exists**, **`contentPath`** must be set (the decoder expects it).

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| **`contentPath`** | string | yes | Directory under the **documentation repo root** where branch output markdown is written (e.g. **`docs`**, **`docs/ad-services`**, **`src/pages`**). Normalized like a POSIX path; **``.``** alone is treated as unset and falls back to **`docs`**. |
| **`outputFilePrefix`** | string | no | Prepended before the base filename for branch output (e.g. **`adr-`**). Combined with **`outputFileName`** / service id; see **Branch output filename** below. |
| **`outputFileDatePrefix`** | boolean | no | If **`true`**, prepend **`yyyy-MM-dd-`** (UTC) to the filename (ADR-style). |

### Branch output filename (JSON + sbt)

Final filename on the documentation branch is built in this order:

1. Optional **date** — `yyyy-MM-dd-` if enabled (**`defaults.docusaurus.outputFileDatePrefix`** or sbt **`autoDocDocusaurusOutputFileDatePrefix`**; sbt **`Some(false)`** forces date off).
2. Optional **custom prefix** — from JSON **`outputFilePrefix`** or sbt **`autoDocDocusaurusOutputFilePrefix`** (sbt non-empty wins).
3. **Base name** — from **`outputFileName`** / **`{{serviceId}}`** / default **`<id>.md`**, with unsafe characters in **`id`** sanitized for the filesystem.

Sbt keys **`autoDocDocumentationRepoKind`**, **`autoDocDocusaurusContentPath`**, **`autoDocDocusaurusOutputFilePrefix`**, and **`autoDocDocusaurusOutputFileDatePrefix`** override the same ideas when set; see [build-settings.md](build-settings.md).

---

## Template placeholders

The markdown template is loaded from **`documentation-repo-root` / `templateRoot` / `markdownTemplate`** when that file exists; otherwise a bundled classpath template is used. Placeholders:

| Placeholder | Replaced with |
| --- | --- |
| **`{{serviceId}}`** | Service **`id`**. |
| **`{{serviceTitle}}`** | **`title`**, or **`id`** if **`title`** is absent. |
| **`{{projectPath}}`** | Path of the sbt project directory relative to the **service** git root (POSIX-style). |
| **`{{generatedAt}}`** | ISO-8601 instant when the file was generated. |
| **`{{changeList}}`** | Markdown list of changed paths under the current project scope from **`git diff --name-status`**, with status labels (added, modified, deleted, …). If none: `_No changes under this project in the current git diff._` |

Git diff scope comes from sbt (**`autoDocGitDiffScope`**, **`autoDocGitBranchBase`**, **`autoDocGitDiffSpec`**), not from this JSON file.

---

## Minimal example

```json
{
  "version": 1,
  "services": [
    {
      "id": "ad-service",
      "title": "Ad Service",
      "pathPrefixes": ["."]
    }
  ],
  "defaults": {
    "markdownTemplate": "default.md.tpl",
    "templateRoot": "templates",
    "outputFileName": "autodoc.md"
  }
}
```

Place **`templates/default.md.tpl`** in the documentation repository. The legacy **`outputFileName`** value **`autodoc.md`** is treated as “use **`<serviceId>.md`**” for Docusaurus branch output.

---

## Docusaurus + ADR-style example

```json
{
  "version": 1,
  "services": [
    {
      "id": "edge-ctrl",
      "title": "Edge Control",
      "pathPrefixes": ["services/edge-ctrl/"]
    }
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

Example branch filename: **`2026-03-31-adr-edge-ctrl.md`** (date is UTC; exact name depends on run date and sanitization).

---

## Validation and errors

- **Missing file** — The plugin reports the expected path and a minimal JSON shape hint.
- **Invalid JSON / decode failure** — Prefix: **`sbt-autodoc: invalid config JSON at …`**
- **Unsupported version** — Only **`version === 1`** is allowed.
- **Unknown service id** — When **`autoDocServiceId`** points at an **`id`** not in **`services`**.
- **No path match** — When no **`pathPrefixes`** match and **`autoDocServiceId`** is unset; fix **`pathPrefixes`** or set **`autoDocServiceId`**.

---

## See also

- [build-settings.md](build-settings.md) — sbt keys that override or extend this JSON.
- [README](../README.md) — quick start and wiring **`ad-service`** to the documentation repo.
