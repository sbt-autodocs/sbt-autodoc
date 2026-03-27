package autodoc.task

import java.io.File

import autodoc.util.ElaborationExecutable
import sbt._

import scala.sys.process._

/**
  * Invokes the [Claude Code](https://docs.anthropic.com/en/docs/claude-code/cli-usage) CLI in print mode
  * (`claude --bare -p ...`) so elaboration runs non-interactively. The prompt instructs the agent to read the
  * autodoc markdown and write elaborated output using Read/Edit tools.
  */
object ClaudeCodeElaboration {

  private val NotFoundHint: String =
    """Install Claude Code and ensure `claude` is on PATH, or set
      |  autoDocElaborationClaudeCodeExecutable := "/full/path/to/claude"
      |If sbt was started from an IDE, PATH may be minimal — use a full path.""".stripMargin

  /** Appended so the agent uses tools instead of only replying in the transcript. */
  private val ToolHint: String =
    """
      |
      |Use the Read tool to load the input file and the Edit tool to write the full elaborated markdown to the output path above.
      |If the output file does not exist yet, create it. Do not only summarize in chat — the deliverable must be saved to the output path.
      |""".stripMargin

  def buildClaudePrompt(basePrompt: String): String =
    basePrompt + ToolHint

  /**
    * @param workDir  working directory for the process (typically project baseDirectory)
    * @return exit code (0 = success)
    */
  def run(
      log: Logger,
      workDir: File,
      executable: String,
      promptText: String,
      extraArgs: Seq[String],
  ): Int = {
    val resolved =
      ElaborationExecutable.resolve(executable, NotFoundHint).fold(sys.error, identity)
    val cmd = Seq(resolved, "--bare", "-p", promptText) ++ extraArgs ++ Seq(
      "--allowedTools",
      "Read,Edit",
    )
    log.info(s"sbt-autodoc: running $resolved --bare -p ... (${cmd.length} args)")
    Process(cmd, workDir) ! ProcessLogger(log.info(_), log.warn(_))
  }
}
