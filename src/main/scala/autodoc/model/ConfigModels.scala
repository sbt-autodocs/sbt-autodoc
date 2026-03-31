package autodoc.model

import io.circe.Decoder
import io.circe.generic.semiauto._

final case class AutoDocConfig(
    version: Int,
    services: List[ServiceConfig],
    defaults: Option[DefaultsConfig],
)

final case class ServiceConfig(
    id: String,
    title: Option[String],
    pathPrefixes: List[String],
)

final case class DefaultsConfig(
    markdownTemplate: Option[String],
    outputFileName: Option[String],
    templateRoot: Option[String],
    /** When unset, consumers may use sbt `autoDocDocumentationRepoKind` or default to `generic`. */
    documentationRepoKind: Option[String],
    docusaurus: Option[DocusaurusDefaults],
)

/** Docusaurus: generated markdown is written under [[contentPath]] relative to the documentation repo root. */
final case class DocusaurusDefaults(
    contentPath: String,
    /** Prepended before the resolved filename (e.g. `adr-` → `adr-edge-ctrl.md`). */
    outputFilePrefix: Option[String],
    /** If true, prepend `yyyy-MM-dd-` before the prefix and base name (ADR-style dated files). */
    outputFileDatePrefix: Option[Boolean],
)

object AutoDocConfig {
  implicit val decoder: Decoder[AutoDocConfig] = deriveDecoder[AutoDocConfig]
}

object ServiceConfig {
  implicit val decoder: Decoder[ServiceConfig] = deriveDecoder[ServiceConfig]
}

object DefaultsConfig {
  implicit val decoder: Decoder[DefaultsConfig] = deriveDecoder[DefaultsConfig]
}

object DocusaurusDefaults {
  implicit val decoder: Decoder[DocusaurusDefaults] = deriveDecoder[DocusaurusDefaults]
}
