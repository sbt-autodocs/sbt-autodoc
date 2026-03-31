package autodoc.config

import autodoc.util.LogUtils
import sbt.Logger

import java.io.File

/** Resolves the documentation repository root (local path, sibling clone, or cached clone). */
object DocumentationRootResolver {

  def resolve(
      log: Logger,
      documentationRepoUrl: Option[String],
      localDocumentationRoot: Option[File],
      documentationRef: String,
      documentationCacheDirectory: File,
      serviceProjectRoot: File,
      repoResolution: String,
  ): Either[String, File] =
    (localDocumentationRoot, documentationRepoUrl) match {
      case (Some(root), _) =>
        if (root.isDirectory) Right(root)
        else Left(s"sbt-autodoc: autoDocLocalDocumentationRoot is not a directory: ${root.getAbsolutePath}")
      case (None, Some(u)) =>
        val layout = repoResolution.trim.toLowerCase.replaceAll("_", "")
        if (layout == "cache")
          DocumentationRepo.ensureCheckout(u, documentationRef, documentationCacheDirectory, log)
        else if (layout == "sibling" || layout.isEmpty)
          resolveSiblingOrClone(log, u, documentationRef, serviceProjectRoot)
        else
          Left(s"sbt-autodoc: unknown autoDocDocumentationRepoResolution: $repoResolution (use sibling or cache)")
      case (None, None) =>
        Left(
          "sbt-autodoc: set autoDocDocumentationRepoUrl (git URL for ad-service-documentation) " +
            "or autoDocLocalDocumentationRoot (local checkout path)",
        )
    }

  /**
    * Uses `../<repo-name>/` next to the service project root (same parent directory).
    * If that path is a git repo: fetch, checkout ref, pull --ff-only.
    * If it does not exist: shallow-clone there.
    */
  private def resolveSiblingOrClone(
      log: Logger,
      repoUrl: String,
      documentationRef: String,
      serviceProjectRoot: File,
  ): Either[String, File] =
    DocumentationRepoUrl.inferRepoDirectoryName(repoUrl) match {
      case None =>
        Left(
          "sbt-autodoc: cannot infer documentation repository directory name from the URL " +
            "(expected a path ending in repo-name or repo-name.git); use autoDocLocalDocumentationRoot or autoDocDocumentationRepoResolution := cache",
        )
      case Some(dirName) =>
        val parent = serviceProjectRoot.getCanonicalFile.getParentFile
        if (parent == null)
          Left("sbt-autodoc: cannot place sibling documentation repo (service project root has no parent directory)")
        else {
          val sibling = new File(parent, dirName)
          val gitMarker = new File(sibling, ".git")
          if (sibling.isDirectory && gitMarker.exists) {
            LogUtils.info(
              log,
              s"sbt-autodoc: using sibling documentation repo ${sibling.getAbsolutePath}",
            )
            DocumentationRepo.fetchCheckoutPull(sibling, documentationRef, log).map(_ => sibling)
          } else if (!sibling.exists) {
            LogUtils.info(
              log,
              s"sbt-autodoc: cloning documentation repo next to project: ${sibling.getAbsolutePath}",
            )
            DocumentationRepo.cloneShallowTo(repoUrl, documentationRef, sibling, log)
          } else
            Left(
              s"sbt-autodoc: path exists but is not a git repository (remove it or use a different URL): ${sibling.getAbsolutePath}",
            )
        }
    }
}
