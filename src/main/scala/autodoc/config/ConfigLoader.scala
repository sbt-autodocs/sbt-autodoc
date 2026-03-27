package autodoc.config

import autodoc.model.AutoDocConfig
import autodoc.util.JsonUtils

import java.io.File

object ConfigLoader {
  def load(file: File): Either[String, AutoDocConfig] =
    JsonUtils.parseJsonFile[AutoDocConfig](file)
}
