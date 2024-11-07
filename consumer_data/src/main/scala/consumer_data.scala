import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.{StreamingQuery, Trigger}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object consumer_data {

  // Class matching the JSON report data
  private case class Citizen(name: String, harmonyscore: Double)
  private case class Location(latitude: Double, longitude: Double)
  private case class Report(harmonywatcher_id: String, current_location: Location, surrounding_citizens: Array[Citizen],
                            words_heard: String, alert: Boolean, timestamp: String)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("KafkaDeltaLakeIntegration")
      .master("local[*]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .getOrCreate()

    import spark.implicits._

    // Define the schema for the data
    val schema = StructType(Seq(
      StructField("harmonywatcher_id", StringType, nullable = false),
      StructField("current_location", StructType(Seq(
        StructField("latitude", DoubleType, nullable = false),
        StructField("longitude", DoubleType, nullable = false)
      )), nullable = false),
      StructField("surrounding_citizens", ArrayType(StructType(Seq(
        StructField("name", StringType, nullable = false),
        StructField("harmonyscore", DoubleType, nullable = false)
      ))), nullable = false),
      StructField("words_heard", StringType, nullable = false),
      StructField("alert", BooleanType, nullable = false),
      StructField("timestamp", StringType, nullable = false)
    ))

    // Read data from Kafka and parse as JSON
    val kafkaStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "report_data")
      .load()
      .select(from_json(col("value").cast("string"), schema).as("data"))
      .select("data.*")
      .as[Report]

    // Write the parsed data to Delta Lake
    val query: StreamingQuery = kafkaStream.writeStream
      .format("delta")
      .outputMode("append")
      .option("checkpointLocation", "../ReadDeltaLake/src/main/data_storage/checkpoint")
      .trigger(Trigger.ProcessingTime("10 seconds")) // Other option : mapGroupsWithState
      .start("../ReadDeltaLake/src/main/data_storage/delta")

    // Await termination
    query.awaitTermination()
  }
}