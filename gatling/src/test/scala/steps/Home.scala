package steps

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder
import globalConfig.Config._

object Home {
  def openApp: ChainBuilder = group("Open_Application") {
    exec(http("Home_Page").get("/").check(status.is(200)))
      .pause(pauseMin, pauseMax)
  }
}