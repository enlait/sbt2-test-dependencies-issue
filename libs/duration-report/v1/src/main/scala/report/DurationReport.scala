package report

import java.time.Duration

import formatter.DurationFormat

object DurationReport {
  /** Renders a duration using the configured formatter library. */
  def render(d: Duration): String = DurationFormat.render(d)
}
