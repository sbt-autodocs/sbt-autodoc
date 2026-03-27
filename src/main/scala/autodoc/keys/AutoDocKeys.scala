package autodoc.keys

import sbt._

trait AutoDocKeys {
  val autoDoc = taskKey[File](
    "Generate markdown from git diff using shared config from ad-service-documentation (or a local clone)",
  )

  /** Git clone URL (e.g. git@github.com:org/ad-service-documentation.git). */
  val autoDocDocumentationRepoUrl =
    settingKey[Option[String]]("Git clone URL for the ad-service-documentation repository")

  /** Use a local checkout instead of cloning (overrides autoDocDocumentationRepoUrl). */
  val autoDocLocalDocumentationRoot =
    settingKey[Option[File]]("Local checkout of ad-service-documentation (skips clone)")

  /** Branch or tag in the documentation repository. */
  val autoDocDocumentationRef = settingKey[String]("Branch or tag to fetch in the documentation repository")

  /** Path to the JSON config file inside the documentation repository. */
  val autoDocDocumentationConfigPath =
    settingKey[String]("Path to autodoc JSON config inside the documentation repo checkout")

  /** Where to clone or update the documentation repository (under target/ by default). */
  val autoDocDocumentationCacheDirectory =
    settingKey[File]("Cache directory for the cloned documentation repository")

  /**
    * If set, passed to `git diff` (e.g. `origin/main...HEAD`). If None, uses uncommitted changes vs `HEAD`
    * (`git diff --name-status HEAD`).
    */
  val autoDocGitDiffSpec =
    settingKey[Option[String]]("Optional git diff spec; default is uncommitted changes vs HEAD")

  /** Pin service id from config instead of inferring from project path. */
  val autoDocServiceId = settingKey[Option[String]]("Optional service id override (must exist in config)")

  /** Output markdown file (defaults to target/autodoc/autodoc.md). */
  val autoDocOutputFile = settingKey[File]("Generated markdown output path")
}

object AutoDocKeys extends AutoDocKeys
