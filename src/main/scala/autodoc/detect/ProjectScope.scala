package autodoc.detect

import autodoc.model.{NameStatusEntry, ScopedChange}
import autodoc.util.FileUtils

import java.io.File

object ProjectScope {
  /** Keep diff entries whose path lies under the sbt project base (relative to git root). */
  def filterToProject(
      gitRoot: File,
      projectBase: File,
      entries: Seq[NameStatusEntry],
  ): Either[String, Seq[ScopedChange]] = {
    val rootPath = gitRoot.getCanonicalFile.toPath
    val basePath = projectBase.getCanonicalFile.toPath
    if (!basePath.startsWith(rootPath))
      return Left(
        s"Project base ${projectBase.getAbsolutePath} is not under git root ${gitRoot.getAbsolutePath}",
      )
    val relBase = FileUtils.posixPathString(rootPath.relativize(basePath))
    val normalized = normalizeSegment(relBase)

    val scoped = entries.flatMap { e =>
      projectRelativePath(gitRoot, normalized, e.path).map { rel =>
        ScopedChange(e, rel)
      }
    }
    Right(scoped)
  }

  private def normalizeSegment(s: String): String =
    if (s == "." || s.isEmpty) "" else s.replace('\\', '/').stripSuffix("/")

  private def projectRelativePath(
      gitRoot: File,
      projectDirFromRoot: String,
      repoPath: String,
  ): Option[String] = {
    val path = repoPath.replace('\\', '/').stripPrefix("/")
    val prefix = projectDirFromRoot
    val under =
      prefix.isEmpty || path == prefix || path.startsWith(prefix + "/")
    if (!under) None
    else {
      val rel =
        if (prefix.isEmpty) path
        else if (path == prefix) "."
        else path.stripPrefix(prefix).stripPrefix("/")
      Some(rel)
    }
  }
}
