package autodoc.config

import autodoc.model.{AutoDocConfig, ServiceConfig}

object ConfigResolver {
  /** Pick the service whose path prefix best matches the project path (longest prefix wins). */
  def resolveService(config: AutoDocConfig, projectPathFromRepoRoot: String): Option[ServiceConfig] = {
    val dir = normalizeDir(projectPathFromRepoRoot)
    val scored = config.services.flatMap { svc =>
      svc.pathPrefixes.flatMap { p =>
        val prefix = normalizePrefix(p)
        if (dir == prefix.dropRight(1) || dir.startsWith(prefix)) Some((prefix.length, svc))
        else None
      }
    }
    scored.sortBy(-_._1).headOption.map(_._2)
  }

  private def normalizeDir(s: String): String = {
    val t = s.replace('\\', '/').stripPrefix("/").stripSuffix("/")
    if (t.isEmpty) "." else t
  }

  private def normalizePrefix(p: String): String = {
    val t = p.replace('\\', '/').stripPrefix("/")
    if (t.endsWith("/")) t else t + "/"
  }
}
