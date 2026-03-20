package userData

import io.gatling.core.Predef._
import io.gatling.core.feeder.BatchableFeederBuilder

object Feeders {
  val userFeeder: BatchableFeederBuilder[String]#F = csv("data/users.csv").circular
}