# Rendering member and AI text: a closed markup subset

Members write descriptions and replies; a model writes verdicts, tutoring and summaries. Both want
lists, emphasis, code — and a model will emit tables, images and links whether asked or not. On a
canvas there is no browser to sanitise HTML for you, and a "full Markdown" library renders whatever
arrives.

**The mine:** markup that grows by configuration. A library flag turned on for one surface
(autolinks, task lists, tables, images) quietly applies everywhere, a parser upgrade adds a
construct, and a model's `![](https://tracker/pixel)` or a raw URL in a member's reply becomes
clickable content that moderation never saw.

## Four decisions

1. **One flavour, one parser, named.** A CommonMark parser that publishes the web target
   (e.g. `org.jetbrains:markdown`); **no GFM**: tables are unreadable at 360 dp, autolinks make any
   URL in a reply clickable, and a `- [ ]` task list is a second checkbox with no storage next to the
   product's real checklist. The parser parses; your code draws.
2. **Your own tree between parser and UI.** The AST maps into a small model; the subset is held by
   **absence of code**, not by configuration — an image cannot be shown because nothing can draw
   one. The mapping is a pure function without Compose, so the subset is tested by plain unit tests.

   ```kotlin
   sealed interface MarkupBlock {
       data class Paragraph(val inlines: List<MarkupInline>) : MarkupBlock
       data class Heading(val level: HeadingLevel, val inlines: List<MarkupInline>) : MarkupBlock   // two visible levels
       data class Bullets(val items: List<List<MarkupBlock>>) : MarkupBlock
       data class Numbers(val start: Int, val items: List<List<MarkupBlock>>) : MarkupBlock
       data class Quote(val blocks: List<MarkupBlock>) : MarkupBlock                               // one level
       data class Code(val source: String) : MarkupBlock
       data class Unsupported(val source: String) : MarkupBlock    // outside the subset: drawn as typed
   }
   sealed interface MarkupInline {
       data class Text(val text: String, val strong: Boolean = false, val emphasis: Boolean = false, val code: Boolean = false) : MarkupInline
       data class Link(val text: String, val href: String) : MarkupInline   // href already passed an https allow-list
       data object Break : MarkupInline
   }
   ```
3. **Unknown is drawn as its source, never dropped.** A table, an image, raw HTML, a setext rule
   come out as the text that was typed. A parser upgrade then cannot lose content, and a feature not
   yet supported (`$x^2$` before formulas exist) is visibly "not understood" instead of empty.
4. **Parse once; policy at render.** The same text is never parsed two ways. What varies is the
   display, along **two independent axes**:

   | Axis      | Values                | Decides                         |
   |-----------|-----------------------|---------------------------------|
   | surface   | `Document` / `Bubble` | heading ladder, block elements  |
   | author    | `Member` / `Model`    | whether a link is clickable     |

   All four combinations exist in practice (description, AI summary, member's report, AI verdict) —
   that is the proof the axes are two. Link policy follows the **author**, never the surface:
   `blocks.forAuthor(Model)` turns every link into plain text (label + URL) after parsing.

## In / out

| In                                         | Out (and why)                                           |
|--------------------------------------------|---------------------------------------------------------|
| paragraph, line break                      | image — a tracking pixel past moderation                |
| strong, emphasis, inline code              | table — unreadable on a phone                           |
| bullet and numbered lists                  | task list — a second checkbox with other semantics      |
| headings (two visible levels)              | autolink — a URL does not become a link by itself       |
| one-level quote                            | raw HTML — returned as text                             |
| fenced code                                | footnotes, reference links, `---` as a divider          |

Opening an external link always goes through a confirmation sheet (the URL is validated in
`features`; the shell's `LinkOpener` only opens) — keep it a regression scenario on every renderer
change.

## Formulas (optional, gated)

If a feature needs maths, a separate pure **gate** decides what may be laid out — a command
allow-list, balanced braces and `\left`/`\right`, limits on length, nesting and count per block; no
`\href`, `\url`, macros, environments, `\text`. Anything that fails the gate stays visible as
monospace source. The gate does not depend on the author (one policy, nothing to drift). Lay out
with your own `Canvas` + `TextMeasurer` view behind one file rather than a LaTeX library when the
library's compressed cost is large (measured: a library spike cost ~+760 KB gzip, an own layout
~+320 KB). The bundled UI font must then carry Greek and the maths operators block —
[compose-design-system / typography-and-fonts.md](../../compose-design-system/references/typography-and-fonts.md).

## Locks (source guards)

- The parser has **one** production call site (the `MarkupText` composable).
- Resource strings never enter marked text (`MarkupText(` and `stringResource(` never on one line):
  UI copy is not markup.
- Every marked bubble names its author (no `Marked(body)` without the author argument).
- The formula delimiter literal appears only in the parser; formula measurement only in the formula
  view; the rejected library's packages appear nowhere.
- Formula examples the server's prompts ask the model to produce are parsed as formulas by the
  client (reads the prompt resources) — prompt and renderer cannot drift apart.

Evidence: family-planner ADR 0045 (closed subset), ADR 0046 (gated formula layout, measured costs),
`designsystem/markup/`, `MarkupSourceGuardTest`, `MarkupGalleryTest`. A dedicated skill for this
renderer may follow; until then this file is the reference.
