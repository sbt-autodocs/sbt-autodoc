package autodoc.git

import autodoc.model.NameStatusEntry

import java.io.File

trait GitClient {
  def repoRoot(start: File): Either[String, File]

  /**
    * `diffSpec` None -> `git diff --name-status HEAD` (uncommitted vs last commit).
    * Some(`origin/main...HEAD`) -> three-dot diff: all commits on current branch since merge-base with `origin/main`.
    */
  def diffNameStatus(start: File, diffSpec: Option[String]): Either[String, Seq[NameStatusEntry]]
}
