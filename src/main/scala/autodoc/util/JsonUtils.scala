package autodoc.util

import io.circe.Decoder
import io.circe.parser.parse

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.util.Try

object JsonUtils {
  def parseJsonFile[A: Decoder](file: File): Either[String, A] =
    for {
      bytes <- Try(Files.readAllBytes(file.toPath)).toEither.left.map(_.getMessage)
      text = new String(bytes, StandardCharsets.UTF_8)
      json <- parse(text).left.map(_.message)
      value <- json.as[A].left.map(_.message)
    } yield value
}
