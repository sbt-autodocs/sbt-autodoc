package autodoc

import autodoc.keys.AutoDocKeys
import autodoc.task.AutoDocTasks
import sbt._

object AutoDocPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = plugins.JvmPlugin

  /** Forwards the same keys as `AutoDocKeys` (avoid `extends AutoDocKeys` here, which would duplicate SettingKey instances). */
  object autoImport {
    val autoDoc = AutoDocKeys.autoDoc
    val autoDocDocumentationRepoUrl = AutoDocKeys.autoDocDocumentationRepoUrl
    val autoDocLocalDocumentationRoot = AutoDocKeys.autoDocLocalDocumentationRoot
    val autoDocDocumentationRef = AutoDocKeys.autoDocDocumentationRef
    val autoDocDocumentationConfigPath = AutoDocKeys.autoDocDocumentationConfigPath
    val autoDocDocumentationRepoKind = AutoDocKeys.autoDocDocumentationRepoKind
    val autoDocDocumentationOutputMode = AutoDocKeys.autoDocDocumentationOutputMode
    val autoDocDocusaurusContentPath = AutoDocKeys.autoDocDocusaurusContentPath
    val autoDocDocusaurusOutputFilePrefix = AutoDocKeys.autoDocDocusaurusOutputFilePrefix
    val autoDocDocusaurusOutputFileDatePrefix = AutoDocKeys.autoDocDocusaurusOutputFileDatePrefix
    val autoDocDocumentationBranchName = AutoDocKeys.autoDocDocumentationBranchName
    val autoDocDocumentationCommitMessage = AutoDocKeys.autoDocDocumentationCommitMessage
    val autoDocDocumentationCommit = AutoDocKeys.autoDocDocumentationCommit
    val autoDocDocumentationGitStage = AutoDocKeys.autoDocDocumentationGitStage
    val autoDocDocumentationBranchMarkdownSource = AutoDocKeys.autoDocDocumentationBranchMarkdownSource
    val autoDocDocumentationRepoResolution = AutoDocKeys.autoDocDocumentationRepoResolution
    val autoDocDocumentationCacheDirectory = AutoDocKeys.autoDocDocumentationCacheDirectory
    val autoDocGitDiffScope = AutoDocKeys.autoDocGitDiffScope
    val autoDocGitBranchBase = AutoDocKeys.autoDocGitBranchBase
    val autoDocGitDiffSpec = AutoDocKeys.autoDocGitDiffSpec
    val autoDocServiceId = AutoDocKeys.autoDocServiceId
    val autoDocPerSubproject = AutoDocKeys.autoDocPerSubproject
    val autoDocOutputFile = AutoDocKeys.autoDocOutputFile
    val autoDocElaborate = AutoDocKeys.autoDocElaborate
    val autoDocElaborationProvider = AutoDocKeys.autoDocElaborationProvider
    val autoDocElaborationMode = AutoDocKeys.autoDocElaborationMode
    val autoDocElaborationPromptFile = AutoDocKeys.autoDocElaborationPromptFile
    val autoDocElaborationOutputFile = AutoDocKeys.autoDocElaborationOutputFile
    val autoDocElaborationAudience = AutoDocKeys.autoDocElaborationAudience
    val autoDocElaborationTone = AutoDocKeys.autoDocElaborationTone
    val autoDocElaborationCustomPrompt = AutoDocKeys.autoDocElaborationCustomPrompt
    val autoDocElaborationCommand = AutoDocKeys.autoDocElaborationCommand
    val autoDocElaborationClaudeCodeExecutable = AutoDocKeys.autoDocElaborationClaudeCodeExecutable
    val autoDocElaborationClaudeCodeArgs = AutoDocKeys.autoDocElaborationClaudeCodeArgs
    val autoDocElaborationCursorCliExecutable = AutoDocKeys.autoDocElaborationCursorCliExecutable
    val autoDocElaborationCursorCliArgs = AutoDocKeys.autoDocElaborationCursorCliArgs
    val autoDocElaborationMermaidDiagrams = AutoDocKeys.autoDocElaborationMermaidDiagrams
    val autoDocElaborationServiceDocs = AutoDocKeys.autoDocElaborationServiceDocs
  }

  override lazy val projectSettings: Seq[Setting[_]] = AutoDocTasks.settings
}
