package globalConfig

object Config {
  val baseUrl: String = System.getProperty("baseUrl", "http://wp:80/")

  // Міняємо логіку на кількість користувачів (Closed Model)
  val targetUsers: Int = System.getProperty("users", "5").toInt
  val rampDuration: Int = System.getProperty("ramp", "1").toInt
  val holdDuration: Int = System.getProperty("hold", "9").toInt // 1 + 9 = 10 хвилин

  val pauseMin = 2
  val pauseMax = 5
}