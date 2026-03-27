# sbt-autodoc

You would rather ship **ad-service** than spend your afternoon in **ad-service-documentation**. This plugin keeps you in the service repo: it pulls the shared JSON config from that docs repo (or a local clone), runs `git diff`, scopes changes to your sbt project, and writes Markdown so you touch the documentation repo as little as possible.

It:

1. Loads configuration from **ad-service-documentation** (or a local checkout you point at).
2. Runs `git diff` and keeps only paths under the **current sbt project** `baseDirectory` (for example, only `ad-service/...` when that project’s base is the service folder).
3. Resolves the **service** (e.g. `ad-service`) from `pathPrefixes` in the config, or from `autoDocServiceId`.
4. Renders a Markdown summary using a template from the documentation repo (or the bundled default).

The plugin is built for **Scala 2.12** (sbt’s own Scala version). It can be used in **Scala 2.12 and 2.13** application builds; those versions only affect your project code, not the plugin.

## Usage

### 1. Publish or depend on the plugin

```scala
// project/plugins.sbt
addSbtPlugin("autodoc" % "sbt-autodoc" % "<version>")
```

### 2. Point at ad-service-documentation

Either clone the repo yourself and set `autoDocLocalDocumentationRoot`, or let the plugin shallow-clone:

```scala
// build.sbt (per project, or in ThisBuild / projectSettings)
autoDocDocumentationRepoUrl := Some("git@github.com:<owner>/ad-service-documentation.git"),
autoDocDocumentationRef := "main",
autoDocDocumentationConfigPath := "autodoc/config.json",
```

### 3. Run the task

```bash
sbt adService/autoDoc
```

(Use your subproject’s id—often `lazy val adService = project.in(file("ad-service"))`.)

Output defaults to `target/autodoc/autodoc.md`.

### Useful settings

| Key | Purpose |
| --- | --- |
| `autoDocLocalDocumentationRoot` | Use a local path instead of cloning (overrides `autoDocDocumentationRepoUrl`). |
| `autoDocGitDiffSpec` | e.g. `Some("origin/main...HEAD")` for branch-range diffs; default is uncommitted vs `HEAD`. |
| `autoDocServiceId` | Force a service id from the JSON config when path-based inference is wrong. |
| `autoDocOutputFile` | Where to write the Markdown file. |

## ad-service-documentation JSON (`version`: 1)

Place a file such as `autodoc/config.json` in that repository:

```json
{
  "version": 1,
  "services": [
    {
      "id": "ad-service",
      "title": "Ad Service",
      "pathPrefixes": ["ad-service/", "services/ad-service/"]
    }
  ],
  "defaults": {
    "markdownTemplate": "default.md.tpl",
    "templateRoot": "templates",
    "outputFileName": "autodoc.md"
  }
}
```

- **pathPrefixes**: Used to infer which service an sbt project belongs to (longest matching prefix wins). Paths are POSIX-style, relative to the git repository root.
- **templates**: Resolved under the documentation repo root, e.g. `templates/default.md.tpl`. Placeholders: `{{serviceId}}`, `{{serviceTitle}}`, `{{projectPath}}`, `{{generatedAt}}`, `{{changeList}}`.

## Development

```bash
sbt compile
```

## License

See [LICENSE](LICENSE).
