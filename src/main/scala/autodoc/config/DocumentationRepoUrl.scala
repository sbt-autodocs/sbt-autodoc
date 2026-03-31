package autodoc.config

/** Derive a sibling directory name from a git remote URL (e.g. `fraud-services-documentation`). */
object DocumentationRepoUrl {

  def inferRepoDirectoryName(url: String): Option[String] = {
    val trimmed = url.trim.stripSuffix("/").stripSuffix(".git")
    if (trimmed.isEmpty) None
    else {
      val lastSegment = trimmed.split('/').lastOption.getOrElse("")
      val name =
        if (lastSegment.contains(':')) lastSegment.split(':').lastOption.getOrElse(lastSegment)
        else lastSegment
      val cleaned = name.stripSuffix(".git").trim
      if (cleaned.nonEmpty) Some(cleaned) else None
    }
  }
}
