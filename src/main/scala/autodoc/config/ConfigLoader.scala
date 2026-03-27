package autodoc.config

import autodoc.model.AutoDocConfig
import autodoc.util.JsonUtils

import java.io.File

object ConfigLoader {
  def load(file: File): Either[String, AutoDocConfig] =
    if (!file.isFile)
      Left(
        s"""sbt-autodoc: config file not found: ${file.getAbsolutePath}
           |Add that file to the documentation repository, or set autoDocDocumentationConfigPath to the real path.
           |Expected JSON: { "version": 1, "services": [ { "id", "pathPrefixes", optional "title" } ], optional "defaults" }""".stripMargin,
      )
    else
      JsonUtils.parseJsonFile[AutoDocConfig](file).left.map { err =>
        s"sbt-autodoc: invalid config JSON at ${file.getAbsolutePath}: $err"
      }
}
