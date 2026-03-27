package autodoc.task

import java.io.File

import autodoc.util.ElaborationExecutable
import sbt._

import scala.sys.process._

/**
  * Invokes the [Cursor CLI](https://cursor.com/docs/cli/headless) Agent in print mode (`agent -p`) with
  * `--force` so the agent can write files (without `--force`, print mode only proposes changes).
  */
object CursorCliElaboration {

  private val NotFoundHint: String =
    """Install the Cursor CLI: https://cursor.com/docs/cli/installation
      |After install, ensure ~/.local/bin is on PATH (`agent --version`), or set the full path in sbt, e.g.
      |  autoDocElaborationCursorCliExecutable := sys.props("user.home") + "/.local/bin/agent"
      |Note: sbt launched from an IDE often inherits a minimal PATH; a full path is the most reliable fix.""".stripMargin

  private val ToolHint: String =
    """
      |
      |Read the input file path above, then write the full elaborated markdown to the output path using your file tools.
      |If the output file does not exist yet, create it. Do not only answer in the transcript — the deliverable must be saved to the output path.
      |""".stripMargin

  def buildCursorPrompt(basePrompt: String): String =
    basePrompt + ToolHint

  private def resolveExecutable(executable: String): String = {
    val trimmed = executable.trim
    ElaborationExecutable.resolve(trimmed, NotFoundHint) match {
      case Right(path) => path
      case Left(msg) if trimmed == "agent" =>
        val fallback = new File(System.getProperty("user.home"), ".local/bin/agent").getPath
        ElaborationExecutable.resolve(fallback, NotFoundHint).fold(sys.error, identity)
      case Left(msg) => sys.error(msg)
    }
  }

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
    val resolved = resolveExecutable(executable)
    val cmd = Seq(resolved, "-p", "--force", promptText) ++ extraArgs
    log.info(s"sbt-autodoc: running $resolved -p --force ... (${cmd.length} args)")
    Process(cmd, workDir) ! ProcessLogger(log.info(_), log.warn(_))
  }
}
