package autodoc.task

import autodoc.keys.AutoDocKeys
import autodoc.render.MarkdownRenderer
import autodoc.util.FileUtils
import sbt.Keys._
import sbt._

import scala.sys.process._

object AutoDocTasks {
  import AutoDocKeys._

  def settings: Seq[Setting[_]] = Seq(
    autoDocDocumentationRepoUrl := None,
    autoDocLocalDocumentationRoot := None,
    autoDocDocumentationRef := "main",
    autoDocDocumentationConfigPath := "autodoc/config.json",
    // One clone per build (root project base, not each module's target/).
    autoDocDocumentationCacheDirectory :=
      (LocalRootProject / baseDirectory).value / "target" / "autodoc" / "documentation-repo",
    autoDocGitDiffScope := "branch",
    autoDocGitBranchBase := "origin/main",
    autoDocGitDiffSpec := None,
    autoDocServiceId := None,
    autoDocOutputFile := target.value / "autodoc" / "autodoc.md",
    autoDocElaborationProvider := "none",
    autoDocElaborationMode := "handoff",
    autoDocElaborationPromptFile := target.value / "autodoc" / "elaboration-prompt.md",
    autoDocElaborationOutputFile := target.value / "autodoc" / "autodoc-elaborated.md",
    autoDocElaborationAudience := "engineering",
    autoDocElaborationTone := "concise",
    autoDocElaborationCustomPrompt := None,
    autoDocElaborationCommand := None,
    autoDocElaborationClaudeCodeExecutable := "claude",
    autoDocElaborationClaudeCodeArgs := Seq.empty,
    autoDocElaborationCursorCliExecutable := "agent",
    autoDocElaborationCursorCliArgs := Seq.empty,
    autoDoc := {
      val log = streams.value.log
      val out = autoDocOutputFile.value
      val gitDiff =
        autoDocGitDiffSpec.value.orElse {
          if (autoDocGitDiffScope.value.equalsIgnoreCase("branch"))
            Some(s"${autoDocGitBranchBase.value}...HEAD")
          else None
        }
      AutoDocRunner
        .run(
          log = log,
          baseDirectory = baseDirectory.value,
          documentationRepoUrl = autoDocDocumentationRepoUrl.value,
          localDocumentationRoot = autoDocLocalDocumentationRoot.value,
          documentationRef = autoDocDocumentationRef.value,
          documentationConfigRelativePath = autoDocDocumentationConfigPath.value,
          documentationCacheDirectory = autoDocDocumentationCacheDirectory.value,
          gitDiffSpec = gitDiff,
          serviceIdOverride = autoDocServiceId.value,
          outputFile = out,
          loader = classOf[MarkdownRenderer].getClassLoader,
        )
        .fold(
          msg => sys.error(msg),
          file => {
            log.success(s"sbt-autodoc: wrote ${file.getAbsolutePath}")
            file
          },
        )
    },
    autoDocElaborate := {
      val log = streams.value.log
      val _ = autoDoc.value
      val provider = autoDocElaborationProvider.value.trim
      if (provider.equalsIgnoreCase("none")) {
        log.info("sbt-autodoc: autoDocElaborationProvider is none; skipping autoDocElaborate")
        Seq.empty
      } else {
        val mode = autoDocElaborationMode.value.trim.toLowerCase
        val input = autoDocOutputFile.value
        val promptFile = autoDocElaborationPromptFile.value
        val outputFile = autoDocElaborationOutputFile.value
        val audience = autoDocElaborationAudience.value
        val tone = autoDocElaborationTone.value
        val custom = autoDocElaborationCustomPrompt.value
        val customBlock =
          custom.map(c => s"\n## Additional instructions\n$c\n").getOrElse("")
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
             |$customBlock
             |""".stripMargin
        val promptForDisk =
          if (provider.equalsIgnoreCase("claude-code"))
            ClaudeCodeElaboration.buildClaudePrompt(promptBody)
          else if (provider.equalsIgnoreCase("cursor-cli"))
            CursorCliElaboration.buildCursorPrompt(promptBody)
          else promptBody
        FileUtils.writeUtf8(promptFile, promptForDisk)
        log.info(s"sbt-autodoc: wrote elaboration prompt ${promptFile.getAbsolutePath}")
        mode match {
          case "execute" =>
            autoDocElaborationCommand.value match {
              case Some(template) =>
                val expanded = template
                  .replace("{input}", input.getAbsolutePath)
                  .replace("{output}", outputFile.getAbsolutePath)
                  .replace("{prompt}", promptFile.getAbsolutePath)
                val exit = expanded ! ProcessLogger(log.info(_), log.warn(_))
                if (exit != 0)
                  sys.error(s"autoDocElaborationCommand exited with $exit")
                log.success(s"sbt-autodoc: elaboration command finished; see ${outputFile.getAbsolutePath}")
                Seq(outputFile)
              case None =>
                if (provider.equalsIgnoreCase("claude-code")) {
                  FileUtils.ensureParentDir(outputFile)
                  val exit = ClaudeCodeElaboration.run(
                    log = log,
                    workDir = baseDirectory.value,
                    executable = autoDocElaborationClaudeCodeExecutable.value,
                    promptText = promptForDisk,
                    extraArgs = autoDocElaborationClaudeCodeArgs.value,
                  )
                  if (exit != 0)
                    sys.error(s"Claude Code CLI exited with code $exit")
                  log.success(s"sbt-autodoc: Claude Code elaboration finished; see ${outputFile.getAbsolutePath}")
                  Seq(outputFile)
                } else if (provider.equalsIgnoreCase("cursor-cli")) {
                  FileUtils.ensureParentDir(outputFile)
                  val exit = CursorCliElaboration.run(
                    log = log,
                    workDir = baseDirectory.value,
                    executable = autoDocElaborationCursorCliExecutable.value,
                    promptText = promptForDisk,
                    extraArgs = autoDocElaborationCursorCliArgs.value,
                  )
                  if (exit != 0)
                    sys.error(s"Cursor CLI (agent) exited with code $exit")
                  log.success(s"sbt-autodoc: Cursor CLI elaboration finished; see ${outputFile.getAbsolutePath}")
                  Seq(outputFile)
                } else
                  sys.error(
                    "autoDocElaborationMode is execute but autoDocElaborationCommand is not set " +
                      "(set a custom command, or use autoDocElaborationProvider := \"claude-code\" or \"cursor-cli\" for built-in CLIs)",
                  )
            }
          case "handoff" =>
            Seq(promptFile)
          case other =>
            sys.error(s"autoDocElaborationMode must be handoff or execute, got: $other")
        }
      }
    },
  )
}
