package autodoc.task

import autodoc.keys.AutoDocKeys
import autodoc.render.MarkdownRenderer
import sbt.Keys._
import sbt._

object AutoDocTasks {
  import AutoDocKeys._

  def settings: Seq[Setting[_]] = Seq(
    autoDocDocumentationRepoUrl := None,
    autoDocLocalDocumentationRoot := None,
    autoDocDocumentationRef := "main",
    autoDocDocumentationConfigPath := "autodoc/config.json",
    autoDocDocumentationCacheDirectory := target.value / "autodoc" / "documentation-repo",
    autoDocGitDiffSpec := None,
    autoDocServiceId := None,
    autoDocOutputFile := target.value / "autodoc" / "autodoc.md",
    autoDoc := {
      val log = streams.value.log
      val out = autoDocOutputFile.value
      AutoDocRunner
        .run(
          log = log,
          baseDirectory = baseDirectory.value,
          documentationRepoUrl = autoDocDocumentationRepoUrl.value,
          localDocumentationRoot = autoDocLocalDocumentationRoot.value,
          documentationRef = autoDocDocumentationRef.value,
          documentationConfigRelativePath = autoDocDocumentationConfigPath.value,
          documentationCacheDirectory = autoDocDocumentationCacheDirectory.value,
          gitDiffSpec = autoDocGitDiffSpec.value,
          serviceIdOverride = autoDocServiceId.value,
          outputFile = out,
          loader = classOf[MarkdownRenderer].getClassLoader,
        )
        .fold(
          msg => sys.error(msg),
          file => {
            log.success(s"sbt-autodoc: wrote ${file.getAbsolutePath}")
            file
          },
        )
    },
  )
}
