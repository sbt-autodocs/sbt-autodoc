package autodoc.model

/** Render-ready document (before template substitution). */
final case class GeneratedDocument(
    title: String,
    bodyMarkdown: String,
)
