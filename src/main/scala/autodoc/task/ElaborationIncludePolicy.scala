package autodoc.task

import sbt.{InteractionService, Logger}

/**
  * Shared `ask` | `include` | `skip` handling for elaboration prompt sections (Mermaid, existing docs, etc.).
  */
object ElaborationIncludePolicy {

  def shouldInclude(
      policy: String,
      itemCount: Int,
      askMessage: String,
      log: Logger,
      interaction: InteractionService,
  ): Boolean = {
    if (itemCount <= 0) return false
    policy.trim.toLowerCase match {
      case "skip" | "no" | "false" => false
      case "include" | "yes" | "true" => true
      case "ask" =>
        System.out.synchronized {
          interaction.confirm(askMessage)
        }
      case other =>
        log.warn(s"sbt-autodoc: unknown elaboration include policy value '$other', treating as skip")
        false
    }
  }
}
