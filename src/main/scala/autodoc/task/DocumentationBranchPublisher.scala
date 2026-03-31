package autodoc.task

import autodoc.config.DocumentationOutputPaths
import autodoc.git.DocumentationGitOps
import autodoc.model.DefaultsConfig
import autodoc.util.{FileUtils, LogUtils}

import java.io.File
import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter

import sbt.Logger

/** Writes Docusaurus markdown to a new branch in the documentation repo (shared by [[AutoDocRunner]] and elaboration). */
object DocumentationBranchPublisher {

  def publish(
      log: Logger,
      docRoot: File,
      defaults: Option[DefaultsConfig],
      serviceId: String,
      markdown: String,
      documentationRepoKindOverride: Option[String],
      docusaurusContentPathOverride: Option[String],
      docusaurusOutputFilePrefix: Option[String],
      docusaurusOutputFileDatePrefix: Option[Boolean],
      documentationBranchName: Option[String],
      documentationCommitMessage: Option[String],
      documentationCommit: Boolean,
      documentationGitStage: Boolean,
  ): Either[String, File] = {
    val kind = DocumentationOutputPaths.effectiveRepoKind(documentationRepoKindOverride, defaults)
    if (kind != "docusaurus")
      Left(
        "sbt-autodoc: documentation branch publish requires Docusaurus; " +
          "set autoDocDocumentationRepoKind := Some(\"docusaurus\") or defaults.documentationRepoKind in JSON",
      )
    else
      for {
        docMd <- DocumentationOutputPaths.docusaurusMarkdownFile(
          docRoot,
          defaults,
          docusaurusContentPathOverride,
          serviceId,
          docusaurusOutputFilePrefix,
          docusaurusOutputFileDatePrefix,
        )
        branchName = documentationBranchName
          .map(_.trim)
          .filter(_.nonEmpty)
          .getOrElse(defaultBranchName(serviceId))
        _ <- DocumentationGitOps.createBranch(docRoot, branchName, log)
        _ = FileUtils.writeUtf8(docMd, markdown)
        rel <- DocumentationOutputPaths.relativePath(docRoot, docMd)
        _ <-
          if (documentationCommit) {
            val msg =
              documentationCommitMessage
                .map(_.trim)
                .filter(_.nonEmpty)
                .getOrElse(s"autodoc: update $serviceId documentation")
            DocumentationGitOps.addAndCommit(docRoot, Seq(rel), msg, log)
          }
          else if (documentationGitStage)
            DocumentationGitOps.addOnly(docRoot, Seq(rel), log)
          else Right(())
        _ = LogUtils.info(
          log,
          if (documentationCommit)
            s"sbt-autodoc: push the documentation branch with: git -C ${docRoot.getAbsolutePath} push -u origin $branchName"
          else if (documentationGitStage)
            s"sbt-autodoc: changes staged only; review then: git -C ${docRoot.getAbsolutePath} commit && git push -u origin $branchName"
          else
            s"sbt-autodoc: file written on branch; stage/commit/push from: ${docRoot.getAbsolutePath} (branch $branchName)",
        )
      } yield docMd
  }

  private def defaultBranchName(serviceId: String): String = {
    val ts =
      DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC)
        .format(Instant.now())
    val raw =
      serviceId.replaceAll("""[^a-zA-Z0-9._-]+""", "-").replaceAll("-+", "-").stripPrefix("-").stripSuffix("-")
    val safe = if (raw.isEmpty) "service" else raw
    s"autodoc/$safe-$ts"
  }
}
