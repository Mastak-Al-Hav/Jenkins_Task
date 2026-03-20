package helpers

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import _root_.globalConfig.Config._
import io.gatling.http.protocol.HttpProtocolBuilder

object BaseHelpers {
  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
}