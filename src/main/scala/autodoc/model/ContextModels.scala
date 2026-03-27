package autodoc.model

import java.io.File

/** Resolved context for rendering documentation. */
final case class DocContext(
    serviceId: String,
    serviceTitle: String,
    projectBase: File,
    gitRoot: File,
    /** Path from git root to project base, POSIX-style, no leading slash. */
    projectPathFromRepoRoot: String,
    scopedChanges: Seq[ScopedChange],
)
