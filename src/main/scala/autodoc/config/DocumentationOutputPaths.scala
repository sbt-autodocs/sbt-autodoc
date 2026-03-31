package autodoc.config

import autodoc.model.DefaultsConfig

import java.io.File
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}

/** Resolves documentation repo kind and Docusaurus output file from JSON + sbt overrides. */
object DocumentationOutputPaths {

  def effectiveRepoKind(
      sbtOverride: Option[String],
      defaults: Option[DefaultsConfig],
  ): String = {
    val fromSbt = sbtOverride.map(_.trim.toLowerCase).filter(_.nonEmpty)
    val fromJson = defaults.flatMap(_.documentationRepoKind).map(_.trim.toLowerCase).filter(_.nonEmpty)
    fromSbt.orElse(fromJson).getOrElse("generic")
  }

  /** Default folder when neither JSON nor sbt sets a path (standard Docusaurus `docs/` root). */
  val DefaultDocusaurusContentPath = "docs"

  /**
    * Markdown filename under the content path. Default: `&lt;serviceId&gt;.md` (e.g. `edge-ctrl.md`).
    * If `defaults.outputFileName` is set to `autodoc.md` (legacy), it is treated as "use service-based name".
    * Any other explicit name is used as-is; you may embed `{{serviceId}}` (e.g. `{{serviceId}}.md`).
    */
  def docusaurusMarkdownFile(
      docRoot: File,
      defaults: Option[DefaultsConfig],
      sbtContentPath: Option[String],
      serviceId: String,
      sbtOutputFilePrefix: Option[String],
      sbtOutputFileDatePrefix: Option[Boolean],
  ): Either[String, File] = {
    val rel =
      normalizedDocusaurusContentPath(
        sbtContentPath.orElse(defaults.flatMap(_.docusaurus).map(_.contentPath)),
      )
    val safeName =
      resolveDocusaurusFileName(defaults, serviceId, sbtOutputFilePrefix, sbtOutputFileDatePrefix)
    Right(fileUnderDocRoot(docRoot, rel, safeName))
  }

  /**
    * Base name from `outputFileName` / `{{serviceId}}`, then optional `yyyy-MM-dd-`, optional prefix, then base.
    * Sbt overrides win when [[sbtOutputFilePrefix]] / [[sbtOutputFileDatePrefix]] are set (use `Some(false)` to force date off).
    */
  def resolveDocusaurusFileName(
      defaults: Option[DefaultsConfig],
      serviceId: String,
      sbtOutputFilePrefix: Option[String],
      sbtOutputFileDatePrefix: Option[Boolean],
  ): String = {
    val id = sanitizeServiceIdForFile(serviceId)
    val baseFileName =
      defaults.flatMap(_.outputFileName).map(_.trim).filter(_.nonEmpty) match {
        case None | Some("autodoc.md") =>
          s"$id.md"
        case Some(template) =>
          template.replace("{{serviceId}}", id).replaceAll("""[\\/]+""", "_")
      }
    val d = defaults.flatMap(_.docusaurus)
    val useDate: Boolean =
      sbtOutputFileDatePrefix.orElse(d.flatMap(_.outputFileDatePrefix)).getOrElse(false)
    val prefixFromConfig: Option[String] = d.flatMap(_.outputFilePrefix).map(_.trim).filter(_.nonEmpty)
    val prefix: String =
      sbtOutputFilePrefix
        .map(_.trim)
        .filter(_.nonEmpty)
        .orElse(prefixFromConfig)
        .getOrElse("")
    val withPrefix = joinCustomPrefixAndBase(prefix, baseFileName)
    if (useDate) {
      val day = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE)
      s"$day-$withPrefix"
    }
    else
      withPrefix
  }

  /** Ensures a single separator between custom prefix and base filename when prefix is non-empty. */
  private def joinCustomPrefixAndBase(prefix: String, baseFileName: String): String = {
    val p = sanitizeFilePrefix(prefix)
    if (p.isEmpty) baseFileName
    else if (p.endsWith("-") || p.endsWith("_")) p + baseFileName
    else p + "-" + baseFileName
  }

  private def sanitizeFilePrefix(s: String): String =
    s.replaceAll("""[^a-zA-Z0-9._-]+""", "-").replaceAll("-+", "-").stripPrefix("-").stripSuffix("-")

  def sanitizeServiceIdForFile(serviceId: String): String = {
    val raw =
      serviceId.replaceAll("""[^a-zA-Z0-9._-]+""", "-").replaceAll("-+", "-").stripPrefix("-").stripSuffix("-")
    if (raw.isEmpty) "service" else raw
  }

  /**
    * Normalizes user input so `src/pages`, `src\pages`, `./src/pages/` all map to `src/pages`.
    * A lone `.` or empty path falls back to [[DefaultDocusaurusContentPath]] (avoids writing to repo root by mistake).
    */
  def normalizedDocusaurusContentPath(raw: Option[String]): String = {
    val segments = raw
      .map(_.trim)
      .filter(_.nonEmpty)
      .map(pathSegments)
      .getOrElse(Seq.empty)
    val filtered = segments.filterNot(_ == ".")
    if (filtered.isEmpty) DefaultDocusaurusContentPath
    else filtered.mkString("/")
  }

  private def pathSegments(s: String): Seq[String] =
    s.replace('\\', '/').trim.stripPrefix("./").split("/").toSeq.map(_.trim).filter(_.nonEmpty)

  private def fileUnderDocRoot(docRoot: File, relativePosix: String, fileName: String): File = {
    val parts = pathSegments(relativePosix)
    val parent = parts.foldLeft(docRoot)((dir, seg) => new File(dir, seg))
    new File(parent, fileName)
  }

  def relativePath(docRoot: File, file: File): Either[String, String] = {
    val root = docRoot.getCanonicalFile.toPath
    val path = file.getCanonicalFile.toPath
    if (!path.startsWith(root))
      Left(s"sbt-autodoc: output file ${file.getAbsolutePath} is not under documentation root ${docRoot.getAbsolutePath}")
    else
      Right(autodoc.util.FileUtils.posixPathString(root.relativize(path)))
  }
}
