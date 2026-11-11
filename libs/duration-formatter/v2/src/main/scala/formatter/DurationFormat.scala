package formatter

import java.time.Duration

object DurationFormat {
  /** Renders a duration as human-readable text. */
  def render(d: Duration): String = {
    val total = d.getSeconds
    if (total < 60) s"${total}s"
    else s"${total / 60}m ${total % 60}s"
  }
}
