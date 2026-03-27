package autodoc.util

import java.io.File

/**
  * Resolves a CLI binary the same way [[java.lang.ProcessBuilder]] does: absolute/relative paths as-is,
  * bare names only via `PATH`. Produces clearer errors than `IOException: error=2`.
  */
object ElaborationExecutable {

  def resolve(binary: String, whenNotFoundHint: String): Either[String, String] = {
    val name = binary.trim
    if (name.isEmpty)
      return Left("sbt-autodoc: elaboration executable setting is empty")

    val asFile = new File(name)
    val treatAsPath = asFile.isAbsolute || name.indexOf(File.separatorChar) >= 0

    if (treatAsPath) {
      val abs = asFile.getAbsoluteFile
      if (abs.isFile && abs.canExecute) Right(abs.getPath)
      else
        Left(
          s"sbt-autodoc: elaboration executable not found or not executable: ${abs.getPath}\n$whenNotFoundHint",
        )
    } else {
      val path = Option(System.getenv("PATH")).getOrElse("")
      val dirs = path.split(File.pathSeparator).filter(_.nonEmpty)
      val found = dirs.map(d => new File(d, name)).find(f => f.isFile && f.canExecute)
      found
        .map(_.getAbsolutePath)
        .toRight(
          s"""sbt-autodoc: '$name' not found on PATH (no executable file in PATH entries).
             |$whenNotFoundHint""".stripMargin,
        )
    }
  }
}
