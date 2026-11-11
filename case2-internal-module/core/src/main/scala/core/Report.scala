package core

import java.time.Duration

import formatter.DurationFormat

object Report {
  def render(d: Duration): String = DurationFormat.render(d)
}
