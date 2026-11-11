package app

import java.time.Duration

import report.DurationReport

object Report {
  def render(d: Duration): String = DurationReport.render(d)
}
