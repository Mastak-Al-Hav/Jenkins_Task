package steps

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import globalConfig.Config._
import io.gatling.core.structure.ChainBuilder

object Cart {

  def addToCart(): ChainBuilder = group("Add_To_Cart_Action") {
    exec(http("Add product to cart")
      .post("/wp-admin/admin-ajax.php")
      .formParam("action", "ic_add_to_cart")
      .formParam("add_cart_data", "current_product=${productId}&cart_content={%22${productId}__%22:1}&current_quantity=1")
      .formParam("cart_container", "0")
      .formParam("cart_widget", "0")
      .check(status.is(200))
      .check(substring("Added!").exists))
      .pause(pauseMin, pauseMax)
  }

  def openCartPage: ChainBuilder = group("Open_Cart") {
    exec(http("Open_Cart_Page")
      .get("/cart")
      .check(regex("""name=['"]total_net['"]\s*value=['"]([^'"]+)['"]""").saveAs("totalNet"))
      .check(regex("""value=['"]([^'"]+)['"]\s*name=['"]trans_id['"]""").saveAs("transId"))
      .check(regex("""name=['"]cart_content['"]\s*value=['"]([^'"]+)['"]""").optional.saveAs("cartContent")))
      .pause(pauseMin, pauseMax)
  }
}