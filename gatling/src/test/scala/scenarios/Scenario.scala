package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import userData.Feeders
import io.gatling.core.structure.ScenarioBuilder
import steps._
import globalConfig.Config._

object Scenario {
  val mainScenario: ScenarioBuilder = scenario("Scenario_addTable_50%_addChair_30%_SubmitOrder")
    // ПРИБРАНО .forever {}
    .exec(flushHttpCache)
    .exec(flushCookieJar)
    .exitBlockOnFail {
      feed(Feeders.userFeeder)
        .exec(Home.openApp)
        .exec(Table.navigateTables)
        .exec(Table.selectTable)
        .exec(Cart.addToCart())

        .randomSwitch(
          50.0 -> exec(
            Chair.navigateChairs,
            Chair.selectChair,
            Cart.addToCart()
          )
        )

        .randomSwitch(
          30.0 -> exec(
            Cart.openCartPage,
            Order.submitCheckoutForm,
            Order.selectState
          )
        )
    }
    .pause(pauseMin, pauseMax)
}