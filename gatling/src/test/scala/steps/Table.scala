package steps

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder
import globalConfig.Config._
import scala.util.Random

object Table {

  def navigateTables: ChainBuilder = group("Navigate_Tables") {
    exec(http("Tables_Category_Page")
      .get("/products-category/tables-2")
      .check(status.is(200))
      .check(regex("""href="(http[^"]+/products/[^"]+)"""").findAll.saveAs("All_Tables_Urls")))
      .pause(pauseMin, pauseMax)
  }

  def selectTable: ChainBuilder = group("Open_Table_Product") {
    exec { session =>
      val urls = session("All_Tables_Urls").asOption[Seq[String]].getOrElse(Nil)
      if (urls.nonEmpty) {
        val selectedUrl = urls(Random.nextInt(urls.size))
        session.set("Random_Table_Url", selectedUrl)
      } else session
    }
      .exec(http("View_Table_Details")
        .get("${Random_Table_Url}")

        .check(status.is(200))
        .check(regex("""name=['"]current_product['"][\s\S]*?value=['"](\d+)['"]""").saveAs("productId")))
      .pause(pauseMin, pauseMax)
  }
}