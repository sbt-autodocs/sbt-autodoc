package autodoc.git

import autodoc.model.NameStatusEntry

object GitParsers {
  def parseNameStatusLines(lines: Seq[String]): Either[String, Seq[NameStatusEntry]] = {
    val parsed = lines.filter(_.trim.nonEmpty).map(parseLine)
    val errs = parsed.collect { case Left(e) => e }
    if (errs.nonEmpty) Left(errs.mkString("; "))
    else Right(parsed.collect { case Right(x) => x })
  }

  private def parseLine(line: String): Either[String, NameStatusEntry] = {
    val parts = line.split('\t').toList
    parts match {
      case status :: path :: Nil =>
        Right(NameStatusEntry(status.trim, path.trim, None))
      case status :: oldPath :: newPath :: Nil if status.headOption.exists(c => c == 'R' || c == 'C') =>
        Right(NameStatusEntry(status.take(1), newPath.trim, Some(oldPath.trim)))
      case _ =>
        Left(s"Unrecognized git name-status line: $line")
    }
  }
}
