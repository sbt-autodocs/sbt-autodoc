package autodoc

import autodoc.keys.AutoDocKeys
import autodoc.task.AutoDocTasks
import sbt._

object AutoDocPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  object autoImport extends AutoDocKeys

  override lazy val projectSettings: Seq[Setting[_]] = AutoDocTasks.settings
}
