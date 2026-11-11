package formatter

import java.time.Duration

object DurationFormat {
  /** Renders a duration as human-readable text. */
  def render(d: Duration): String = s"${d.getSeconds}s"
}
