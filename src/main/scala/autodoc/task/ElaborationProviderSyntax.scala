package autodoc.task

/** Normalizes [[autodoc.keys.AutoDocKeys.autoDocElaborationProvider]] for comparisons. */
private[autodoc] object ElaborationProviderSyntax {

  /** Canonical: `none`, `claude-code`, `cursor-cli`, or other trimmed lowercase string. */
  def normalize(raw: String): String = {
    val p =
      raw
        .trim
        .toLowerCase
        .replace('\u2011', '-')
        .replace('\u2010', '-')
    p match {
      case ""          => "none"
      case "cursor" | "cursor_cli" => "cursor-cli"
      case "claude" | "claude_code" => "claude-code"
      case other       => other
    }
  }
}
