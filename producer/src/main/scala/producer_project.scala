import java.util.Properties
import scala.io.Source
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer
import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps

import scala.annotation.tailrec
import scala.util.Random

object producer_project {
  private case class Location(latitude: Double, longitude: Double)
  private case class Citizen(name: String, harmonyscore: Double)
  private case class Report(harmonywatcher_id: String, current_location: Location, surrounding_citizens: List[Citizen],
                            words_heard: String, alert: Boolean, timestamp: String)
  private case class Reports(reports: List[Report])

  def main(args: Array[String]): Unit = {

    // Load reports from JSON file
    val source = Source.fromFile("src/main/resources/reports.json")
    val jsonString = try source.mkString finally source.close()
    val reports = decode[Reports](jsonString).getOrElse(Reports(Nil)).reports

    // Kafka producer properties
    val props: Properties = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])

    val producer: KafkaProducer[String, String] = new KafkaProducer[String, String](props)

    // Recursive function to send individual reports
    def sendRandomReport(reports: List[Report], producer: KafkaProducer[String, String]): Unit = {
      if (reports.nonEmpty) {
        val randomReport = reports(Random.nextInt(reports.length))

        // Sending to "report_data" topic
        val record = new ProducerRecord[String, String]("report_data", "report", randomReport.asJson.noSpaces)
        producer.send(record, (recordMetadata: RecordMetadata, e: Exception) => {
          if (e != null) {
            e.printStackTrace()
          } else {
            println(s"Sent record to report_data: $recordMetadata")
          }
        })

        // Sending to "report_alert" topic if alert is true
        if (randomReport.alert) {
          val alertRecord = new ProducerRecord[String, String]("report_alert", "report", randomReport.asJson.noSpaces)
          producer.send(alertRecord, (recordMetadata: RecordMetadata, e: Exception) => {
            if (e != null) {
              e.printStackTrace()
            } else {
              println(s"Sent record to report_alert: $recordMetadata")
            }
          })
        }
      }
    }

    // Recursive function to send reports over a minute
    @tailrec
    def sendReportsOverMinute(numReports: Int, timeInMillis: Long = 180000 * 10): Unit = {
      if (numReports > 0) {
        val sleepTime = Random.nextInt(timeInMillis.toInt / numReports)
        Thread.sleep(sleepTime)

        sendRandomReport(reports, producer)

        sendReportsOverMinute(numReports - 1, timeInMillis - sleepTime)
      }
    }

    // Initial call to send reports over a given time
    sendReportsOverMinute(3000 * 10)
  }
}
