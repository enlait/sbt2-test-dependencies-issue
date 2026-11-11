package app

import java.time.Duration

object Report {
  def render(d: Duration): String = core.Report.render(d)
}
