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
