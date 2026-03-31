package autodoc.task

import sbt.{InteractionService, Logger}

/**
  * Whether to add Mermaid / `.mmd` instructions to the elaboration prompt.
  * - `include` — always add when `.mmd` files exist
  * - `skip` — never add
  * - `ask` — uses sbt [[InteractionService]] (not `System.console`, which blocks under sbt)
  */
object ElaborationDiagramPolicy {

  def shouldInclude(
      policy: String,
      mmdCount: Int,
      log: Logger,
      interaction: InteractionService,
  ): Boolean = {
    val msg =
      s"sbt-autodoc: Found $mmdCount .mmd file(s) in the documentation repo. " +
        "Include diagram-update instructions in the elaboration prompt?"
    ElaborationIncludePolicy.shouldInclude(policy, mmdCount, msg, log, interaction)
  }
}
