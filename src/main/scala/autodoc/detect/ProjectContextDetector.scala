package autodoc.detect

import autodoc.config.ConfigResolver
import autodoc.model.{AutoDocConfig, ServiceConfig}

object ProjectContextDetector {
  def resolveService(
      config: AutoDocConfig,
      projectPathFromRepoRoot: String,
      serviceIdOverride: Option[String],
  ): Either[String, ServiceConfig] =
    serviceIdOverride match {
      case Some(id) =>
        config.services.find(_.id == id).toRight(s"sbt-autodoc: unknown service id '$id' in documentation config")
      case None =>
        ConfigResolver
          .resolveService(config, projectPathFromRepoRoot)
          .toRight(
            s"sbt-autodoc: no service mapping for project path '$projectPathFromRepoRoot'; " +
              "add pathPrefixes in ad-service-documentation config or set autoDocServiceId",
          )
    }
}
