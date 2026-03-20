package steps

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder
import globalConfig.Config._
import scala.util.Random

object Chair {

  def navigateChairs: ChainBuilder = group("Navigate_Chairs") {
    exec(http("Navigate_Chairs_Page")
      .get("/chairs")
      .check(status.is(200))
      .check(regex("""href="(http[^"]+/products/[^"]+)"""").findAll.saveAs("All_Chairs_Urls")))
      .pause(pauseMin, pauseMax)
  }

  def selectChair: ChainBuilder = group("View_Chair") {
    exec { session =>
      val urls = session("All_Chairs_Urls").asOption[Seq[String]].getOrElse(Nil)
      if (urls.nonEmpty) {
        val selectedUrl = urls(Random.nextInt(urls.size))
        session.set("Random_Chair_Url", selectedUrl)
      } else session
    }
      .exec(http("View_Chair_Details")
        .get("${Random_Chair_Url}")
        .check(status.is(200))

        .check(regex("""name=['"]current_product['"][\s\S]*?value=['"](\d+)['"]""").saveAs("productId")))
      .pause(pauseMin, pauseMax)
  }
}