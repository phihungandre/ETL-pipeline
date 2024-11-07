import java.sql.{Connection, DriverManager}
import java.util.Properties
import scala.io.Source
import org.json4s._
import org.json4s.native.JsonMethods._
import scalaj.http.Http

object Main extends App {

  private case class Citizen(name: String, harmonyscore: Double)

  private def loadConfig(filename: String): Map[String, String] = {
    val lines = Source.fromFile(filename).getLines()
    lines.map { line =>
      val Array(key, value) = line.split("=")
      key -> value
    }.toMap
  }

  private def sendToWebhook(message: String, embed: String, webhookUrl: String): Unit = {
    Http(webhookUrl)
      .postData(s"""{"content": "$message", "embeds": [$embed]}""")
      .header("Content-Type", "application/json")
      .asString
  }

  private def parseCitizensJson(jsonString: String): List[Citizen] = {
    implicit val formats: DefaultFormats.type = DefaultFormats
    parse(jsonString).extract[List[Citizen]]
  }

  private def citizensToString(citizens: List[Citizen]): String = {
    citizens.map(citizen => s"${citizen.name} (${citizen.harmonyscore})").mkString(", ")
  }

  try {
    val config = loadConfig("src/main/env/.env")

    val url = "jdbc:postgresql://" + config("DBHOST") + "/" + config("DBNAME")
    val props = new Properties()
    props.setProperty("user", config("DBUSER"))
    props.setProperty("password", config("DBUSERPASSWORD"))

    val conn: Connection = DriverManager.getConnection(url, props)

    val stmt = conn.createStatement()
    stmt.execute("LISTEN report_insert")

    // Dans le cadre du POC on a fait une boucle infinie, mais en prod on peut utiliser un scheduler, ex : Hikari
    while (true) {
      val pgconn = conn.unwrap(classOf[org.postgresql.PGConnection])
      val notify = pgconn.getNotifications(5000)

      if (notify != null) {
        val rs = stmt.executeQuery("SELECT current_location_latitude, current_location_longitude, " +
          "surrounding_citizens_temp, timestamp FROM reports ORDER BY id DESC LIMIT 1 FOR UPDATE;")
        if (rs.next()) {
          val latitude = rs.getString("current_location_latitude")
          val longitude = rs.getString("current_location_longitude")
          val citizensJson = rs.getString("surrounding_citizens_temp")
          val timestamp = rs.getString("timestamp")

          val citizens = parseCitizensJson(citizensJson)

          val embed = s"""
            {
              "title": "GO ARREST!",
              "description": "Some peasants aren't so harmonious anymore... A patrol must be sent to:",
              "color": 3447003,
              "fields": [
                {"name": "Location:", "value": "Latitude: $latitude, Longitude: $longitude"},
                {"name": "Inharmonious citizens:", "value": "${citizensToString(citizens)}"},
                {"name": "Caught at:", "value": "$timestamp"}
              ]
            }
          """

          sendToWebhook("", embed, config("WEBHOOKURL"))
        }
      }
    }
  } catch {
    case e: Exception => e.printStackTrace()
  }
}
