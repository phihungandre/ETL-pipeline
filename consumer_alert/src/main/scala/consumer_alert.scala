import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{col, from_json, struct, to_json, to_timestamp, to_utc_timestamp}
import org.apache.spark.sql.types._

object consumer_alert {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("KafkaPostgreSQLIntegration")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val kafkaStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "report_alert")
      .load()

    // Definition of the schema corresponding to the Report case class
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
      StructField("timestamp", TimestampType, nullable = false)
    ))

    val query = kafkaStream
      .selectExpr("CAST(value AS STRING) AS json")
      .select(from_json($"json", schema).as("data"))
      .select("data.*")
      .writeStream
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        // Convert surrounding_citizens to json and then cast it to jsonb
        val reportAlertDF = batchDF.select(
          $"harmonywatcher_id",
          $"current_location.latitude".alias("current_location_latitude"),
          $"current_location.longitude".alias("current_location_longitude"),
          $"words_heard",
          to_json(col("surrounding_citizens")).alias("surrounding_citizens_temp"), // Write to temporary column
          $"alert",
          $"timestamp"
        )

        // Write to report_alert table
        reportAlertDF.write
          .format("jdbc")
          .option("url", "jdbc:postgresql://localhost/reports")
          .option("dbtable", "reports")
          .option("user", "inde")
          .option("password", "inde")
          .mode("append")
          .save()
      }
      .start()

    query.awaitTermination()
  }
}

