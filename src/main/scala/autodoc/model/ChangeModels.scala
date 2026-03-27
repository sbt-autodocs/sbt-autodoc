package autodoc.model

/** One line from `git diff --name-status`. */
final case class NameStatusEntry(
    status: String,
    path: String,
    oldPath: Option[String],
)

/** A change scoped to the current sbt project directory. */
final case class ScopedChange(
    entry: NameStatusEntry,
    /** Path relative to the project base directory (POSIX-style). */
    projectRelativePath: String,
)
