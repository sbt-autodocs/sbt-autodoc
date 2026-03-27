package autodoc.git

import autodoc.model.NameStatusEntry

import java.io.File

trait GitClient {
  def repoRoot(start: File): Either[String, File]

  /** `diffSpec` examples: None -> compare working tree + index to HEAD; Some("origin/main...HEAD") */
  def diffNameStatus(start: File, diffSpec: Option[String]): Either[String, Seq[NameStatusEntry]]
}
