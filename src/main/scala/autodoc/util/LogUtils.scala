package autodoc.util

import sbt.Logger

object LogUtils {
  def info(log: Logger, message: => String): Unit = log.info(message)
  def warn(log: Logger, message: => String): Unit = log.warn(message)
}
