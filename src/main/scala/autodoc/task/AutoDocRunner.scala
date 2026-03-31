package autodoc.task

import autodoc.config.{
  ConfigLoader,
  DocumentationRootResolver,
}
import autodoc.detect.{ProjectContextDetector, ProjectScope}
import autodoc.git.GitCliClient
import autodoc.model.{AutoDocConfig, DocContext, ScopedChange, ServiceConfig}
import autodoc.render.MarkdownRenderer
import autodoc.util.{FileUtils, LogUtils}

import java.io.File

import sbt.Logger

object AutoDocRunner {

  /** Documentation repo root and resolved service id (for elaboration prompts). */
  final case class ElaborationDocumentationContext(docRoot: File, serviceId: String)

  /** Resolves the same documentation checkout and service as [[run]] / [[publishDocumentationBranchFromElaboratedMarkdown]]. */
  def loadElaborationDocumentationContext(
      log: Logger,
      baseDirectory: File,
      documentationRepoUrl: Option[String],
      localDocumentationRoot: Option[File],
      documentationRef: String,
      documentationConfigRelativePath: String,
      documentationCacheDirectory: File,
      serviceProjectRoot: File,
      documentationRepoResolution: String,
      gitDiffSpec: Option[String],
      serviceIdOverride: Option[String],
  ): Either[String, ElaborationDocumentationContext] = {
    val git = new GitCliClient()
    loadDocumentationContext(
      log,
      git,
      baseDirectory,
      documentationRepoUrl,
      localDocumentationRoot,
      documentationRef,
      documentationConfigRelativePath,
      documentationCacheDirectory,
      serviceProjectRoot,
      documentationRepoResolution,
      gitDiffSpec,
      serviceIdOverride,
    ).map(l => ElaborationDocumentationContext(l.docRoot, l.service.id))
  }

  /** Result of loading the documentation repo + service mapping (no markdown yet). */
  private final case class DocContextLoaded(
      docRoot: File,
      config: AutoDocConfig,
      service: ServiceConfig,
      gitRoot: File,
      projectPath: String,
      scopedChanges: Seq[ScopedChange],
  )

  def run(
      log: Logger,
      baseDirectory: File,
      documentationRepoUrl: Option[String],
      localDocumentationRoot: Option[File],
      documentationRef: String,
      documentationConfigRelativePath: String,
      documentationCacheDirectory: File,
      serviceProjectRoot: File,
      documentationRepoResolution: String,
      gitDiffSpec: Option[String],
      serviceIdOverride: Option[String],
      outputFile: File,
      loader: ClassLoader,
      documentationRepoKindOverride: Option[String],
      documentationOutputMode: String,
      docusaurusContentPathOverride: Option[String],
      docusaurusOutputFilePrefix: Option[String],
      docusaurusOutputFileDatePrefix: Option[Boolean],
      documentationBranchName: Option[String],
      documentationCommitMessage: Option[String],
      documentationCommit: Boolean,
      documentationGitStage: Boolean,
      documentationBranchMarkdownSource: String,
  ): Either[String, File] = {
    val git = new GitCliClient()
    for {
      loaded <- loadDocumentationContext(
        log,
        git,
        baseDirectory,
        documentationRepoUrl,
        localDocumentationRoot,
        documentationRef,
        documentationConfigRelativePath,
        documentationCacheDirectory,
        serviceProjectRoot,
        documentationRepoResolution,
        gitDiffSpec,
        serviceIdOverride,
      )
      ctx = DocContext(
        serviceId = loaded.service.id,
        serviceTitle = loaded.service.title.getOrElse(loaded.service.id),
        projectBase = baseDirectory,
        gitRoot = loaded.gitRoot,
        projectPathFromRepoRoot = loaded.projectPath,
        scopedChanges = loaded.scopedChanges,
      )
      renderer = new MarkdownRenderer(loader)
      md <- renderer.renderMarkdown(loaded.docRoot, loaded.config.defaults, loaded.service, ctx)
      _ = FileUtils.writeUtf8(outputFile, md)
      primary <- publishDocumentationBranchIfRequested(
        log,
        loaded,
        md,
        documentationRepoKindOverride,
        documentationOutputMode,
        docusaurusContentPathOverride,
        docusaurusOutputFilePrefix,
        docusaurusOutputFileDatePrefix,
        documentationBranchName,
        documentationCommitMessage,
        documentationCommit,
        documentationGitStage,
        documentationBranchMarkdownSource,
      )
    } yield primary.getOrElse(outputFile)
  }

  /**
    * Publishes elaborated markdown to the documentation branch (call from [[autoDocElaborate]] when
    * [[documentationBranchMarkdownSource]] is `elaborated`).
    */
  def publishDocumentationBranchFromElaboratedMarkdown(
      log: Logger,
      baseDirectory: File,
      documentationRepoUrl: Option[String],
      localDocumentationRoot: Option[File],
      documentationRef: String,
      documentationConfigRelativePath: String,
      documentationCacheDirectory: File,
      serviceProjectRoot: File,
      documentationRepoResolution: String,
      gitDiffSpec: Option[String],
      serviceIdOverride: Option[String],
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
    val git = new GitCliClient()
    for {
      loaded <- loadDocumentationContext(
        log,
        git,
        baseDirectory,
        documentationRepoUrl,
        localDocumentationRoot,
        documentationRef,
        documentationConfigRelativePath,
        documentationCacheDirectory,
        serviceProjectRoot,
        documentationRepoResolution,
        gitDiffSpec,
        serviceIdOverride,
      )
      out <- DocumentationBranchPublisher.publish(
        log,
        loaded.docRoot,
        loaded.config.defaults,
        loaded.service.id,
        markdown,
        documentationRepoKindOverride,
        docusaurusContentPathOverride,
        docusaurusOutputFilePrefix,
        docusaurusOutputFileDatePrefix,
        documentationBranchName,
        documentationCommitMessage,
        documentationCommit,
        documentationGitStage,
      )
    } yield out
  }

  private def loadDocumentationContext(
      log: Logger,
      git: GitCliClient,
      baseDirectory: File,
      documentationRepoUrl: Option[String],
      localDocumentationRoot: Option[File],
      documentationRef: String,
      documentationConfigRelativePath: String,
      documentationCacheDirectory: File,
      serviceProjectRoot: File,
      documentationRepoResolution: String,
      gitDiffSpec: Option[String],
      serviceIdOverride: Option[String],
  ): Either[String, DocContextLoaded] =
    for {
      docRoot <- DocumentationRootResolver.resolve(
        log,
        documentationRepoUrl,
        localDocumentationRoot,
        documentationRef,
        documentationCacheDirectory,
        serviceProjectRoot,
        documentationRepoResolution,
      )
      config <- ConfigLoader.load(new File(docRoot, documentationConfigRelativePath))
      _ <- Either.cond(config.version == 1, (), s"Unsupported autodoc config version: ${config.version}")
      gitRoot <- git.repoRoot(baseDirectory)
      entries <- git.diffNameStatus(baseDirectory, gitDiffSpec)
      projectPath = relativePathFromRepoRoot(gitRoot, baseDirectory)
      scoped <- ProjectScope.filterToProject(gitRoot, baseDirectory, entries)
      service <- ProjectContextDetector.resolveService(config, projectPath, serviceIdOverride)
    } yield DocContextLoaded(
      docRoot = docRoot,
      config = config,
      service = service,
      gitRoot = gitRoot,
      projectPath = projectPath,
      scopedChanges = scoped,
    )

  private def publishDocumentationBranchIfRequested(
      log: Logger,
      loaded: DocContextLoaded,
      md: String,
      documentationRepoKindOverride: Option[String],
      documentationOutputMode: String,
      docusaurusContentPathOverride: Option[String],
      docusaurusOutputFilePrefix: Option[String],
      docusaurusOutputFileDatePrefix: Option[Boolean],
      documentationBranchName: Option[String],
      documentationCommitMessage: Option[String],
      documentationCommit: Boolean,
      documentationGitStage: Boolean,
      documentationBranchMarkdownSource: String,
  ): Either[String, Option[File]] = {
    val mode = documentationOutputMode.trim.toLowerCase.replaceAll("_", "")
    val source = documentationBranchMarkdownSource.trim.toLowerCase.replaceAll("_", "")
    if (mode != "documentationbranch")
      Right(None)
    else if (source == "elaborated") {
      LogUtils.info(
        log,
        "sbt-autodoc: documentation branch will receive elaborated markdown after autoDocElaborate (execute); " +
          "or set autoDocDocumentationBranchMarkdownSource := \"generated\" to publish raw output from autoDoc only.",
      )
      Right(None)
    }
    else if (source == "generated" || source.isEmpty)
      DocumentationBranchPublisher
        .publish(
          log,
          loaded.docRoot,
          loaded.config.defaults,
          loaded.service.id,
          md,
          documentationRepoKindOverride,
          docusaurusContentPathOverride,
          docusaurusOutputFilePrefix,
          docusaurusOutputFileDatePrefix,
          documentationBranchName,
          documentationCommitMessage,
          documentationCommit,
          documentationGitStage,
        )
        .map(Some(_))
    else
      Left(s"sbt-autodoc: unknown autoDocDocumentationBranchMarkdownSource: $documentationBranchMarkdownSource")
  }

  private def relativePathFromRepoRoot(gitRoot: File, projectBase: File): String = {
    val root = gitRoot.getCanonicalFile.toPath
    val base = projectBase.getCanonicalFile.toPath
    FileUtils.posixPathString(root.relativize(base))
  }
}
