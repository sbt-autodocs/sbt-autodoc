package autodoc.task

import autodoc.config.DocumentationRootResolver
import autodoc.keys.AutoDocKeys
import autodoc.render.MarkdownRenderer
import autodoc.util.FileUtils
import sbt.Keys._
import sbt._

import java.io.IOException

import scala.sys.process._

object AutoDocTasks {
  import AutoDocKeys._

  def settings: Seq[Setting[_]] = Seq(
    autoDocDocumentationRepoUrl := None,
    autoDocLocalDocumentationRoot := None,
    autoDocDocumentationRef := "main",
    autoDocDocumentationConfigPath := "autodoc/config.json",
    autoDocDocumentationRepoKind := None,
    autoDocDocumentationOutputMode := "serviceTarget",
    autoDocDocusaurusContentPath := None,
    autoDocDocusaurusOutputFilePrefix := None,
    autoDocDocusaurusOutputFileDatePrefix := None,
    autoDocDocumentationBranchName := None,
    autoDocDocumentationCommitMessage := None,
    autoDocDocumentationCommit := true,
    autoDocDocumentationGitStage := false,
    autoDocDocumentationBranchMarkdownSource := "elaborated",
    autoDocDocumentationRepoResolution := "sibling",
    // One clone per build (root project base, not each module's target/).
    autoDocDocumentationCacheDirectory :=
      (LocalRootProject / baseDirectory).value / "target" / "autodoc" / "documentation-repo",
    autoDocGitDiffScope := "branch",
    autoDocGitBranchBase := "origin/main",
    autoDocGitDiffSpec := None,
    autoDocServiceId := None,
    autoDocPerSubproject := false,
    autoDocOutputFile := {
      val t =
        if (autoDocPerSubproject.value) target.value
        else (LocalRootProject / target).value
      t / "autodoc" / "autodoc.md"
    },
    autoDocElaborationProvider := "none",
    autoDocElaborationMode := "handoff",
    autoDocElaborationPromptFile := {
      val t =
        if (autoDocPerSubproject.value) target.value
        else (LocalRootProject / target).value
      t / "autodoc" / "elaboration-prompt.md"
    },
    autoDocElaborationOutputFile := {
      val t =
        if (autoDocPerSubproject.value) target.value
        else (LocalRootProject / target).value
      t / "autodoc" / "autodoc-elaborated.md"
    },
    autoDocElaborationAudience := "engineering",
    autoDocElaborationTone := "concise",
    autoDocElaborationCustomPrompt := None,
    autoDocElaborationCommand := None,
    autoDocElaborationClaudeCodeExecutable := "claude",
    autoDocElaborationClaudeCodeArgs := Seq.empty,
    autoDocElaborationCursorCliExecutable := "agent",
    autoDocElaborationCursorCliArgs := Seq.empty,
    autoDocElaborationMermaidDiagrams := "ask",
    autoDocElaborationServiceDocs := "ask",
    autoDoc := {
      val log = streams.value.log
      val out = autoDocOutputFile.value
      val per = autoDocPerSubproject.value
      val rootBase = (LocalRootProject / baseDirectory).value.getCanonicalFile
      val isRoot = baseDirectory.value.getCanonicalFile == rootBase
      if (!per && !isRoot) {
        log.info(
          "sbt-autodoc: skipping autoDoc on nested project (single-repo mode). " +
            "Run `autoDoc` on the root project, or set autoDocPerSubproject := true for per-module outputs.",
        )
        out
      } else {
        val scopeBase =
          if (per) baseDirectory.value
          else (LocalRootProject / baseDirectory).value
        val localRootBase = (LocalRootProject / baseDirectory).value.getCanonicalFile
        val gitDiff =
          autoDocGitDiffSpec.value.orElse {
            if (autoDocGitDiffScope.value.equalsIgnoreCase("branch"))
              Some(s"${autoDocGitBranchBase.value}...HEAD")
            else None
          }
        AutoDocRunner
          .run(
            log = log,
            baseDirectory = scopeBase,
            documentationRepoUrl = autoDocDocumentationRepoUrl.value,
            localDocumentationRoot = autoDocLocalDocumentationRoot.value,
            documentationRef = autoDocDocumentationRef.value,
            documentationConfigRelativePath = autoDocDocumentationConfigPath.value,
            documentationCacheDirectory = autoDocDocumentationCacheDirectory.value,
            serviceProjectRoot = localRootBase,
            documentationRepoResolution = autoDocDocumentationRepoResolution.value,
            gitDiffSpec = gitDiff,
            serviceIdOverride = autoDocServiceId.value,
            outputFile = out,
            loader = classOf[MarkdownRenderer].getClassLoader,
            documentationRepoKindOverride = autoDocDocumentationRepoKind.value,
            documentationOutputMode = autoDocDocumentationOutputMode.value,
            docusaurusContentPathOverride = autoDocDocusaurusContentPath.value,
            docusaurusOutputFilePrefix = autoDocDocusaurusOutputFilePrefix.value,
            docusaurusOutputFileDatePrefix = autoDocDocusaurusOutputFileDatePrefix.value,
            documentationBranchName = autoDocDocumentationBranchName.value,
            documentationCommitMessage = autoDocDocumentationCommitMessage.value,
            documentationCommit = autoDocDocumentationCommit.value,
            documentationGitStage = autoDocDocumentationGitStage.value,
            documentationBranchMarkdownSource = autoDocDocumentationBranchMarkdownSource.value,
          )
          .fold(
            msg => sys.error(msg),
            file => {
              log.success(s"sbt-autodoc: wrote ${file.getAbsolutePath}")
              file
            },
          )
      }
    },
    autoDocElaborate := {
      val log = streams.value.log
      val interaction = interactionService.value
      val per = autoDocPerSubproject.value
      val rootBase = (LocalRootProject / baseDirectory).value.getCanonicalFile
      val isRoot = baseDirectory.value.getCanonicalFile == rootBase
      val _ = autoDoc.value
      if (!per && !isRoot) {
        log.info(
          "sbt-autodoc: skipping autoDocElaborate on nested project (single-repo mode). " +
            "Run on the root project, or set autoDocPerSubproject := true.",
        )
        Seq.empty
      } else {
        val elaborationWorkDir =
          if (per) baseDirectory.value
          else (LocalRootProject / baseDirectory).value
        val providerRaw = autoDocElaborationProvider.value
        val provider = ElaborationProviderSyntax.normalize(providerRaw)
        if (provider == "none") {
          log.info("sbt-autodoc: autoDocElaborationProvider is none; skipping autoDocElaborate")
          Seq.empty
        } else {
        log.info(
          s"sbt-autodoc: elaboration — project=${name.value}, autoDocElaborationProvider raw='$providerRaw' normalized='$provider', " +
            s"autoDocElaborationCommand=${autoDocElaborationCommand.value}",
        )
        val localRootBase = (LocalRootProject / baseDirectory).value.getCanonicalFile
        val gitDiffBranch =
          autoDocGitDiffSpec.value.orElse {
            if (autoDocGitDiffScope.value.equalsIgnoreCase("branch"))
              Some(s"${autoDocGitBranchBase.value}...HEAD")
            else None
          }
        val input = autoDocOutputFile.value
        val promptFile = autoDocElaborationPromptFile.value
        val outputFile = autoDocElaborationOutputFile.value
        def publishDocumentationBranchAfterElaborate(handoffMode: Boolean): Unit = {
          val outMode = autoDocDocumentationOutputMode.value.trim.toLowerCase.replaceAll("_", "")
          val src = autoDocDocumentationBranchMarkdownSource.value.trim.toLowerCase.replaceAll("_", "")
          if (outMode != "documentationbranch" || src != "elaborated") ()
          else
            FileUtils.readUtf8(outputFile) match {
              case Right(md) if md.trim.nonEmpty =>
                AutoDocRunner
                  .publishDocumentationBranchFromElaboratedMarkdown(
                    log = log,
                    baseDirectory = elaborationWorkDir,
                    documentationRepoUrl = autoDocDocumentationRepoUrl.value,
                    localDocumentationRoot = autoDocLocalDocumentationRoot.value,
                    documentationRef = autoDocDocumentationRef.value,
                    documentationConfigRelativePath = autoDocDocumentationConfigPath.value,
                    documentationCacheDirectory = autoDocDocumentationCacheDirectory.value,
                    serviceProjectRoot = localRootBase,
                    documentationRepoResolution = autoDocDocumentationRepoResolution.value,
                    gitDiffSpec = gitDiffBranch,
                    serviceIdOverride = autoDocServiceId.value,
                    markdown = md,
                    documentationRepoKindOverride = autoDocDocumentationRepoKind.value,
                    docusaurusContentPathOverride = autoDocDocusaurusContentPath.value,
                    docusaurusOutputFilePrefix = autoDocDocusaurusOutputFilePrefix.value,
                    docusaurusOutputFileDatePrefix = autoDocDocusaurusOutputFileDatePrefix.value,
                    documentationBranchName = autoDocDocumentationBranchName.value,
                    documentationCommitMessage = autoDocDocumentationCommitMessage.value,
                    documentationCommit = autoDocDocumentationCommit.value,
                    documentationGitStage = autoDocDocumentationGitStage.value,
                  )
                  .fold(
                    sys.error,
                    f =>
                      log.success(
                        s"sbt-autodoc: documentation branch updated with elaborated markdown at ${f.getAbsolutePath}",
                      ),
                  )
              case Right(_) =>
                if (handoffMode)
                  log.warn(
                    "sbt-autodoc: elaborated output file is still empty (handoff mode only writes the prompt). " +
                      "Use autoDocElaborationMode := \"execute\" to run the AI and publish to the documentation repo in one step, " +
                      "or write " + outputFile.getAbsolutePath + " yourself and run autoDocElaborate again. " +
                      "Alternatively set autoDocDocumentationBranchMarkdownSource := \"generated\" so autoDoc publishes raw markdown to the docs branch.",
                  )
                else
                  log.warn(
                    "sbt-autodoc: elaborated output is empty; skipping documentation branch publish",
                  )
              case Left(err) =>
                log.warn(s"sbt-autodoc: could not read elaborated markdown ($err); skipping documentation branch publish")
            }
        }
        val mode = autoDocElaborationMode.value.trim.toLowerCase
        val audience = autoDocElaborationAudience.value
        val tone = autoDocElaborationTone.value
        val custom = autoDocElaborationCustomPrompt.value
        val customBlock =
          custom.map(c => s"\n## Additional instructions\n$c\n").getOrElse("")
        val diagramBlock = {
          val docRoot = DocumentationRootResolver.resolve(
            log,
            autoDocDocumentationRepoUrl.value,
            autoDocLocalDocumentationRoot.value,
            autoDocDocumentationRef.value,
            autoDocDocumentationCacheDirectory.value,
            (LocalRootProject / baseDirectory).value.getCanonicalFile,
            autoDocDocumentationRepoResolution.value,
          )
          docRoot match {
            case Left(msg) =>
              log.debug(s"sbt-autodoc: documentation repo not resolved for .mmd scan: $msg")
              ""
            case Right(root) =>
              val mmds = MermaidDiagramIndex.findMmdFiles(root)
              if (mmds.isEmpty) ""
              else if (
                ElaborationDiagramPolicy.shouldInclude(
                  autoDocElaborationMermaidDiagrams.value,
                  mmds.size,
                  log,
                  interaction,
                )
              ) {
                log.info(
                  s"sbt-autodoc: adding Mermaid diagram hints (${mmds.size} .mmd file(s)) to elaboration prompt",
                )
                MermaidDiagramIndex.formatDiagramSection(mmds, root)
              }
              else ""
          }
        }
        val serviceDocsBlock =
          AutoDocRunner
            .loadElaborationDocumentationContext(
              log = log,
              baseDirectory = elaborationWorkDir,
              documentationRepoUrl = autoDocDocumentationRepoUrl.value,
              localDocumentationRoot = autoDocLocalDocumentationRoot.value,
              documentationRef = autoDocDocumentationRef.value,
              documentationConfigRelativePath = autoDocDocumentationConfigPath.value,
              documentationCacheDirectory = autoDocDocumentationCacheDirectory.value,
              serviceProjectRoot = localRootBase,
              documentationRepoResolution = autoDocDocumentationRepoResolution.value,
              gitDiffSpec = gitDiffBranch,
              serviceIdOverride = autoDocServiceId.value,
            ) match {
              case Left(msg) =>
                log.debug(s"sbt-autodoc: documentation context not resolved for service-doc scan: $msg")
                ""
              case Right(ctx) =>
                val related = ServiceDocumentationIndex.findRelatedFiles(ctx.docRoot, ctx.serviceId)
                if (related.isEmpty) ""
                else if (
                  ElaborationIncludePolicy.shouldInclude(
                    autoDocElaborationServiceDocs.value,
                    related.size,
                    s"sbt-autodoc: Found ${related.size} existing documentation file(s) for `${ctx.serviceId}` " +
                      "in the documentation repo. Include in-place update instructions in the elaboration prompt?",
                    log,
                    interaction,
                  )
                ) {
                  log.info(
                    s"sbt-autodoc: adding existing service doc hints (${related.size} file(s), service=${ctx.serviceId}) to elaboration prompt",
                  )
                  ServiceDocumentationIndex.formatServiceSection(related, ctx.docRoot, ctx.serviceId)
                }
                else ""
            }
        val promptBody =
          s"""# Autodoc AI elaboration ($provider)
             |
             |Read the generated autodoc markdown at:
             |`${input.getAbsolutePath}`
             |
             |Write elaborated documentation to:
             |`${outputFile.getAbsolutePath}`
             |
             |- **Audience**: $audience
             |- **Tone**: $tone
             |$customBlock$diagramBlock$serviceDocsBlock
             |""".stripMargin
        val promptForDisk =
          if (provider == "claude-code")
            ClaudeCodeElaboration.buildClaudePrompt(promptBody)
          else if (provider == "cursor-cli")
            CursorCliElaboration.buildCursorPrompt(promptBody)
          else promptBody
        FileUtils.writeUtf8(promptFile, promptForDisk)
        log.info(s"sbt-autodoc: wrote elaboration prompt ${promptFile.getAbsolutePath}")
        mode match {
          case "execute" =>
            autoDocElaborationCommand.value match {
              case Some(template) =>
                log.info(
                  "sbt-autodoc: autoDocElaborationCommand is set — only that shell runs; " +
                    "built-in claude-code / cursor-cli executables are not used for this task.",
                )
                val expanded = template
                  .replace("{input}", input.getAbsolutePath)
                  .replace("{output}", outputFile.getAbsolutePath)
                  .replace("{prompt}", promptFile.getAbsolutePath)
                val exit =
                  try expanded ! ProcessLogger(log.info(_), log.warn(_))
                  catch {
                    case e: IOException =>
                      sys.error(
                        s"sbt-autodoc: failed to start autoDocElaborationCommand: ${e.getMessage}\n" +
                          "If you meant the Cursor CLI, the binary is usually `agent`, not `cursor-cli`. " +
                          "Remove autoDocElaborationCommand and use autoDocElaborationProvider := \"cursor-cli\" " +
                          "with autoDocElaborationCursorCliExecutable := \"agent\" (or the full path to agent). " +
                          "See README (Cursor CLI / Cannot run program \"agent\").",
                      )
                  }
                if (exit != 0)
                  sys.error(s"autoDocElaborationCommand exited with $exit")
                log.success(s"sbt-autodoc: elaboration command finished; see ${outputFile.getAbsolutePath}")
                publishDocumentationBranchAfterElaborate(handoffMode = false)
                Seq(outputFile)
              case None =>
                log.info(
                  s"sbt-autodoc: built-in elaboration (no autoDocElaborationCommand): provider=$provider " +
                    "(claude-code → claude CLI, cursor-cli → Cursor agent)",
                )
                if (provider == "claude-code") {
                  FileUtils.ensureParentDir(outputFile)
                  val exit = ClaudeCodeElaboration.run(
                    log = log,
                    workDir = elaborationWorkDir,
                    executable = autoDocElaborationClaudeCodeExecutable.value,
                    promptText = promptForDisk,
                    extraArgs = autoDocElaborationClaudeCodeArgs.value,
                  )
                  if (exit != 0)
                    sys.error(s"Claude Code CLI exited with code $exit")
                  log.success(s"sbt-autodoc: Claude Code elaboration finished; see ${outputFile.getAbsolutePath}")
                  publishDocumentationBranchAfterElaborate(handoffMode = false)
                  Seq(outputFile)
                } else if (provider == "cursor-cli") {
                  FileUtils.ensureParentDir(outputFile)
                  val exit = CursorCliElaboration.run(
                    log = log,
                    workDir = elaborationWorkDir,
                    executable = autoDocElaborationCursorCliExecutable.value,
                    promptText = promptForDisk,
                    extraArgs = autoDocElaborationCursorCliArgs.value,
                  )
                  if (exit != 0)
                    sys.error(s"Cursor CLI (agent) exited with code $exit")
                  log.success(s"sbt-autodoc: Cursor CLI elaboration finished; see ${outputFile.getAbsolutePath}")
                  publishDocumentationBranchAfterElaborate(handoffMode = false)
                  Seq(outputFile)
                } else
                  sys.error(
                    "autoDocElaborationMode is execute but autoDocElaborationCommand is not set " +
                      "(set a custom command, or use autoDocElaborationProvider := \"claude-code\" or \"cursor-cli\" for built-in CLIs)",
                  )
            }
          case "handoff" =>
            publishDocumentationBranchAfterElaborate(handoffMode = true)
            Seq(promptFile)
          case other =>
            sys.error(s"autoDocElaborationMode must be handoff or execute, got: $other")
        }
        }
      }
    },
  )
}
