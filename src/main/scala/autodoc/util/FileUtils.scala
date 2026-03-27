package autodoc.util

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

object FileUtils {
  def ensureParentDir(file: File): Unit = {
    val p = file.getParentFile
    if (p != null && !p.exists()) p.mkdirs()
  }

  def writeUtf8(file: File, content: String): Unit = {
    ensureParentDir(file)
    Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8))
  }

  def readUtf8(file: File): Either[String, String] =
    try Right(new String(Files.readAllBytes(file.toPath), StandardCharsets.UTF_8))
    catch { case e: Exception => Left(e.getMessage) }

  /** Normalize to forward slashes, strip leading slash, no trailing slash except root "." */
  def posixPathString(path: Path): String = {
    val s = path.toString.replace('\\', '/').stripPrefix("/")
    if (s.isEmpty) "." else s
  }
}
