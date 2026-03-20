package steps

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.ChainBuilder
import globalConfig.Config._

object Order {

  def submitCheckoutForm: ChainBuilder = group("Submit_Checkout") {
    doIf(session => session.contains("cartContent")) {
      exec(http("Submit_Checkout_Form")
        .post("/checkout")
        .formParam("cart_name", "${fullName}")
        .formParam("cart_address", "${address}")
        .formParam("cart_postal", "${postal}")
        .formParam("cart_city", "${city}")
        .formParam("cart_country", "${country}")
        .formParam("cart_phone", "${phone}")
        .formParam("cart_email", "${email}")
        .formParam("cart_type", "order")
        .formParam("cart_submit", "Place Order")
        .formParam("total_net", "${totalNet}")
        .formParam("trans_id", "${transId}")
        .formParam("cart_content", "${cartContent}")
        .check(status.is(200)))
    }
      .pause(pauseMin, pauseMax)
  }

  def selectState: ChainBuilder = group("State_Dropdown") {
    exec(http("State_Dropdown")
      .post("/wp-admin/admin-ajax.php")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .formParam("action", "ic_state_dropdown")
      .formParam("country_code", "${country}")
      .formParam("state_code", "")
      .check(status.is(200)))
      .pause(pauseMin, pauseMax)
  }
}