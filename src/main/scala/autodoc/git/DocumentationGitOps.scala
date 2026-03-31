package autodoc.git

import autodoc.util.LogUtils
import sbt.Logger

import java.io.File
import scala.sys.process._

/**
  * Git operations inside the documentation repository checkout (branch + commit for PR workflow).
  */
object DocumentationGitOps {

  def createBranch(docRoot: File, branchName: String, log: Logger): Either[String, Unit] = {
    LogUtils.info(log, s"sbt-autodoc: creating branch $branchName in ${docRoot.getAbsolutePath}")
    val code = Process(Seq("git", "-C", docRoot.getAbsolutePath, "checkout", "-b", branchName)).!(
      ProcessLogger(log.info(_), log.warn(_)),
    )
    if (code == 0) Right(())
    else Left(s"git checkout -b $branchName failed with exit code $code (branch may already exist; set autoDocDocumentationBranchName)")
  }

  private def gitAdd(docRoot: File, pathsRelativeToRepo: Seq[String], log: Logger): Either[String, Unit] = {
    if (pathsRelativeToRepo.isEmpty) Right(())
    else {
      val addArgs = Seq("git", "-C", docRoot.getAbsolutePath, "add", "--") ++ pathsRelativeToRepo
      val codeAdd = Process(addArgs).!(ProcessLogger(log.info(_), log.warn(_)))
      if (codeAdd == 0) Right(())
      else Left(s"git add failed with exit code $codeAdd")
    }
  }

  /** Stage only the given paths (no commit). */
  def addOnly(docRoot: File, pathsRelativeToRepo: Seq[String], log: Logger): Either[String, Unit] = {
    if (pathsRelativeToRepo.isEmpty) Right(())
    else {
      LogUtils.info(
        log,
        s"sbt-autodoc: staging only (no commit): ${pathsRelativeToRepo.mkString(", ")} in ${docRoot.getAbsolutePath}",
      )
      gitAdd(docRoot, pathsRelativeToRepo, log)
    }
  }

  def addAndCommit(
      docRoot: File,
      pathsRelativeToRepo: Seq[String],
      message: String,
      log: Logger,
  ): Either[String, Unit] = {
    if (pathsRelativeToRepo.isEmpty) Right(())
    else {
      gitAdd(docRoot, pathsRelativeToRepo, log).flatMap { _ =>
        LogUtils.info(log, s"sbt-autodoc: git commit -m ... in ${docRoot.getAbsolutePath}")
        val codeCommit =
          Process(
            Seq("git", "-C", docRoot.getAbsolutePath, "commit", "-m", message),
          ).!(ProcessLogger(log.info(_), log.warn(_)))
        if (codeCommit == 0) Right(())
        else Left(s"git commit failed with exit code $codeCommit (configure user.name and user.email for this repo if needed)")
      }
    }
  }
}
