package app

import java.time.Duration

class ReportSpec extends munit.FunSuite {
  test("renders a duration in seconds") {
    assertEquals(Report.render(Duration.ofSeconds(90)), "90s")
  }
}
