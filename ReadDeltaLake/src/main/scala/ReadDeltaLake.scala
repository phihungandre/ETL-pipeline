import org.apache.spark.sql.{SparkSession, functions}
import org.apache.spark.sql.functions._
import java.sql.Timestamp

object ReadDeltaLake {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("ReadDeltaLake")
      .master("local[*]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .getOrCreate()

    // Path to your Delta Lake
    val deltaLakePath = "src/main/data_storage/delta"

    // Read data from Delta Lake
    val deltaTable = spark.read.format("delta").load(deltaLakePath)

    // Count and print the total number of reports in the Delta Lake
    val totalCount = deltaTable.count()
    println(s"\nTotal number of reports in the Delta Lake: $totalCount \n")

    // Question 1:
    // - Combien de personnes ne sont plus en harmonie durant une journée ?
    // - Quelle est la proportion de personnes non harmonieuses dans notre population ?
    println("Question 1:")

    val alertCount = deltaTable.filter(deltaTable("alert") === true)
      .select(functions.explode(deltaTable("surrounding_citizens")).as("surrounding_citizen"))
      .count()

    println(s"Nombre de personnes n'étant plus en harmonie : $alertCount")

    // Count total unique citizens
    val totalCitizensCount = deltaTable
      .select(functions.explode(deltaTable("surrounding_citizens")).as("surrounding_citizen"))
      .distinct()
      .count()

    // Calculate percentage of non-harmonious citizens
    val nonHarmoniousPercentage = (alertCount.toDouble / totalCitizensCount.toDouble) * 100
    println(s"Pourcentage de personnes non harmonieuses: $nonHarmoniousPercentage%")

    // Question 2:
    // Est ce que le regroupement de personnes non harmonieuses varient au fil du temps ?
    // Y-a-t il des lieux spécifiques qui entrainent les personnes à ne plus devenir harmonieux ?
    println("\nQuestion 2:")

    val alertReportsByCountAndTimestamp = deltaTable
      .filter(deltaTable("alert") === true)
      .groupBy(deltaTable("timestamp"))
      .agg(functions.count("*").alias("alert_count"))
      .orderBy(functions.desc("alert_count"), deltaTable("timestamp"))

    println("Reports with alert = true over time, ordered by alert count and timestamp:")
    alertReportsByCountAndTimestamp.show(false)

    val alertLocations = deltaTable
      .filter(col("alert") === true)
      .groupBy("current_location")
      .agg(count("current_location").alias("nb_occurrence"))
      .orderBy(desc("nb_occurrence"))

    println("Lieux spécifiques entraînant alert=true:")
    alertLocations.show(alertLocations.count().toInt, truncate = false)

    // Question 3: A quelle fréquence des personnes non harmonieuses sont-elles pacifisées ?
    println("\nQuestion 3:")

    val alertTimestamps = deltaTable
      .filter(col("alert") === true)
      .select(col("timestamp").cast("timestamp").alias("timestamp"))

    val minTimestamp = alertTimestamps.agg(min("timestamp")).first().getAs[Timestamp](0)
    val maxTimestamp = alertTimestamps.agg(max("timestamp")).first().getAs[Timestamp](0)
    val uniqueAlertTimestampsCount = alertTimestamps.distinct().count()

    if (uniqueAlertTimestampsCount > 1) {
      val frequencyMillis = (maxTimestamp.getTime - minTimestamp.getTime).toDouble / (uniqueAlertTimestampsCount - 1)

      val hours = (frequencyMillis / (1000 * 60 * 60)).toInt
      val minutes = ((frequencyMillis / (1000 * 60)) % 60).toInt
      val seconds = (frequencyMillis / 1000 % 60).toInt
      val milliseconds = f"${frequencyMillis % 1000}%.3f"

      println(s"Frequency of alerts: $hours hours, $minutes minutes, $seconds seconds and $milliseconds milliseconds per alert")
    } else {
      println("Not enough data to calculate frequency.")
    }


    // Question 4: Les personnes non-harmonieuses ont-elles une tendance à le redevenir ?
    println("\nQuestion 4:")

    val alertsWithSurroundingCitizens = deltaTable
      .filter(deltaTable("alert") === true)
      .select(deltaTable("surrounding_citizens"))
      .filter(functions.size(deltaTable("surrounding_citizens")) > 1)

    val recurringCitizensCount = alertsWithSurroundingCitizens
      .withColumn("citizen", functions.explode(deltaTable("surrounding_citizens")))
      .groupBy("citizen")
      .count()
      .filter(functions.col("count") > 1)
      .count()

    println(s"Number of citizens present in more than one alert: $recurringCitizensCount\n")
  }
}
