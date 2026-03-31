package autodoc.task

import autodoc.config.DocumentationOutputPaths
import autodoc.util.FileUtils

import java.io.File

/**
  * Finds existing documentation files in the documentation repo that refer to the service by name
  * (filename or body text), mirroring the [[MermaidDiagramIndex]] flow for elaboration prompts.
  */
object ServiceDocumentationIndex {

  private val SkipDirs = Set(".git", "target", "node_modules", "build", "dist")

  /** Extensions scanned for service id in file content. */
  private val TextExtensions: Set[String] =
    Set(".md", ".mdx", ".txt", ".rst", ".json", ".yaml", ".yml")

  private val MaxBytesForContentScan = 1024 * 1024

  /**
    * Files to mention in the elaboration prompt: name contains the sanitized service id (e.g. `edge-ctrl.md`),
    * or text content contains the service id (case-insensitive). `.mmd` files are excluded here because they are
    * covered by [[MermaidDiagramIndex]].
    */
  def findRelatedFiles(docRoot: File, serviceId: String): Seq[File] = {
    if (!docRoot.isDirectory) return Seq.empty
    val sanitized = DocumentationOutputPaths.sanitizeServiceIdForFile(serviceId)
    val sanitizedLc = sanitized.toLowerCase
    if (sanitizedLc.isEmpty) return Seq.empty

    val byName = findByFilename(docRoot, sanitizedLc)
    val byContent =
      if (sanitized.length >= 3)
        findByContent(docRoot, serviceId, sanitized)
      else
        Seq.empty

    (byName ++ byContent)
      .filterNot(_.getName.endsWith(".mmd"))
      .map(_.getCanonicalFile)
      .distinct
      .sortBy(_.getAbsolutePath)
  }

  def formatServiceSection(files: Seq[File], docRoot: File, serviceId: String): String = {
    val rootPath = docRoot.getCanonicalFile.toPath
    val lines = files.map { f =>
      val canonical = f.getCanonicalFile
      val rel = FileUtils.posixPathString(rootPath.relativize(canonical.toPath))
      s"- `${canonical.getAbsolutePath}` _(repo-relative: `$rel`)_"
    }.mkString("\n")
    s"""
       |
       |## Existing documentation for this service
       |These paths are **already part of the documentation repository** and reference **`$serviceId`** (in the file name and/or in the text). Update them **in place** when your changes should be reflected there—same idea as editing `.mmd` diagram sources listed above.
       |
       |$lines
       |
       |**Rules**
       |- Prefer a **minimal diff** per file: revise existing sections rather than replacing unrelated pages.
       |- Keep Docusaurus / site conventions (front matter, headings, links) consistent with each file’s current style.
       |- The primary elaboration output still goes to the path given in this prompt (`Write elaborated documentation to:`); align that content with these repo files where they describe the same service, and **do not** duplicate large bodies unnecessarily across paths unless the workflow requires it.
       |""".stripMargin
  }

  private def findByFilename(docRoot: File, sanitizedLc: String): Seq[File] = {
    def walk(dir: File): List[File] = {
      val kids = Option(dir.listFiles()).getOrElse(Array.empty[File])
      kids.toList.flatMap { f =>
        if (f.isDirectory) {
          val n = f.getName
          if (SkipDirs(n)) Nil
          else walk(f)
        } else if (f.isFile && f.getName.toLowerCase.contains(sanitizedLc)) List(f)
        else Nil
      }
    }
    walk(docRoot)
  }

  private def findByContent(docRoot: File, serviceId: String, sanitized: String): Seq[File] = {
    val idLc = serviceId.toLowerCase
    val sanLc = sanitized.toLowerCase

    def walk(dir: File): List[File] = {
      val kids = Option(dir.listFiles()).getOrElse(Array.empty[File])
      kids.toList.flatMap { f =>
        if (f.isDirectory) {
          val n = f.getName
          if (SkipDirs(n)) Nil
          else walk(f)
        } else if (f.isFile && textExtension(f) && f.length() <= MaxBytesForContentScan) {
          FileUtils.readUtf8(f) match {
            case Right(content) if contentReferences(content, idLc, sanLc) => List(f)
            case _                                                          => Nil
          }
        } else Nil
      }
    }
    walk(docRoot)
  }

  private def textExtension(f: File): Boolean = {
    val n = f.getName.toLowerCase
    val dot = n.lastIndexOf('.')
    if (dot < 0) false
    else TextExtensions(n.substring(dot))
  }

  private def contentReferences(content: String, idLc: String, sanLc: String): Boolean = {
    val lc = content.toLowerCase
    lc.contains(idLc) || lc.contains(sanLc)
  }
}
