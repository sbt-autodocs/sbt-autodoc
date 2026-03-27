package autodoc.render

import autodoc.model.{DefaultsConfig, DocContext, ServiceConfig}
import autodoc.util.FileUtils

import java.io.File
import java.time.Instant
import scala.io.Source

class MarkdownRenderer(loader: ClassLoader) extends DocumentRenderer {

  def renderMarkdown(
      documentationRepoRoot: File,
      defaults: Option[DefaultsConfig],
      service: ServiceConfig,
      ctx: DocContext,
  ): Either[String, String] = {
    val templateName =
      defaults.flatMap(_.markdownTemplate).getOrElse("default.md.tpl")
    val templateRoot = defaults.flatMap(_.templateRoot).getOrElse("templates")
    for {
      raw <- loadTemplate(documentationRepoRoot, templateRoot, templateName)
    } yield substitute(raw, service, ctx)
  }

  private def loadTemplate(docRoot: File, templateRoot: String, name: String): Either[String, String] = {
    val external = new File(docRoot, templateRoot + "/" + name)
    if (external.exists()) FileUtils.readUtf8(external)
    else {
      val resource = Option(loader.getResourceAsStream(s"autodoc/templates/$name"))
      resource
        .map { in =>
          try Right(Source.fromInputStream(in, "UTF-8").mkString)
          finally in.close()
        }
        .getOrElse(Left(s"sbt-autodoc: template not found: ${external.getPath} and classpath autodoc/templates/$name"))
    }
  }

  private def substitute(
      template: String,
      service: ServiceConfig,
      ctx: DocContext,
  ): String = {
    val title = service.title.getOrElse(service.id)
    val changes = ctx.scopedChanges
      .map { sc =>
        val label = statusLabel(sc.entry.status)
        s"- **$label** `${sc.projectRelativePath}`"
      }
      .mkString("\n")
    val changeBlock =
      if (changes.nonEmpty) changes
      else "_No changes under this project in the current git diff._"
    template
      .replace("{{serviceId}}", service.id)
      .replace("{{serviceTitle}}", title)
      .replace("{{projectPath}}", ctx.projectPathFromRepoRoot)
      .replace("{{generatedAt}}", Instant.now().toString)
      .replace("{{changeList}}", changeBlock)
  }

  private def statusLabel(status: String): String =
    status.headOption match {
      case Some('A') => "added"
      case Some('M') => "modified"
      case Some('D') => "deleted"
      case Some('R') => "renamed"
      case Some('C') => "copied"
      case _         => status
    }
}
