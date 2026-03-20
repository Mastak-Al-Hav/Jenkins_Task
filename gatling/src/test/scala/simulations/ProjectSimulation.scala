package simulations

import io.gatling.core.Predef._
import helpers.BaseHelpers._
import globalConfig.Config._
import scenarios.Scenario

import scala.concurrent.duration._

class ProjectSimulation extends Simulation {

	val closedModel = Seq(
		// Плавно нарощуємо кількість активних юзерів до цільового показника
		rampConcurrentUsers(1).to(targetUsers).during(rampDuration.minutes),
		// Тримаємо цю кількість стабільною
		constantConcurrentUsers(targetUsers).during(holdDuration.minutes)
	)

	setUp(
		Scenario.mainScenario.inject(closedModel)
	).protocols(httpProtocol)
}