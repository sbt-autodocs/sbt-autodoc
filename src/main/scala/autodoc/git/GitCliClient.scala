package autodoc.git

import autodoc.model.NameStatusEntry

import java.io.File
import scala.sys.process._

class GitCliClient extends GitClient {
  override def repoRoot(start: File): Either[String, File] = {
    val out = runGit(start, Seq("rev-parse", "--show-toplevel"))
    out.map(o => new File(o.trim))
  }

  override def diffNameStatus(start: File, diffSpec: Option[String]): Either[String, Seq[NameStatusEntry]] = {
    val args = diffSpec match {
      case None =>
        Seq("diff", "--name-status", "HEAD")
      case Some(spec) =>
        Seq("diff", "--name-status", spec)
    }
    for {
      raw <- runGit(start, args)
      lines = raw.split("\r\n|\n").toSeq.filter(_.nonEmpty)
      entries <- GitParsers.parseNameStatusLines(lines)
    } yield entries
  }

  private def runGit(cwd: File, args: Seq[String]): Either[String, String] = {
    val cmd = "git" +: args
    try {
      val output = Process(cmd, cwd).!!
      Right(output)
    } catch {
      case e: RuntimeException =>
        Left(s"git ${args.mkString(" ")} failed: ${e.getMessage}")
    }
  }
}
