package com.ct.spark.consumer

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.sql.functions._
import com.ct.spark.model.AuditClass
import com.ct.spark.utils.PropertyFileReader
import com.fasterxml.uuid.Generators
import java.time.format.DateTimeFormatter

object DStream {

  def dstreamConsumer(spark: SparkSession, jobMetadata: AuditClass.JobMetadata) = {

    println("******* Inside dstreamConsumer() **********")

    val bootstrap_servers_config = PropertyFileReader.getPropertyString("bootstrap_servers_config", "fhir-consumer.properties").trim
    val group_id = PropertyFileReader.getPropertyString("group_id", "fhir-consumer.properties").trim
    val protocol = PropertyFileReader.getPropertyString("protocol", "fhir-consumer.properties").trim
    val auditRawPath = PropertyFileReader.getPropertyString("audit_raw_path", "fhir-consumer.properties").trim
    val auditLogPath = PropertyFileReader.getPropertyString("audit_log_path", "fhir-consumer.properties").trim
    val frequency = PropertyFileReader.getPropertyString("frequency_in_sec", "fhir-consumer.properties").trim.toInt
    val out = jobMetadata.destinationDir
    val topics = jobMetadata.topic.split(",").map(_.trim)

    println("*** Topics ===> " + topics)
    println("*** group_id ===> " + group_id)
    println("*** protocol ===> " + protocol)
    println("**** auditRawPath ===> " + auditRawPath)
    println("**** auditLogPath ===> " + auditLogPath)
    println("**** frequency_in_sec ===> " + frequency)

    val ssc = new StreamingContext(spark.sparkContext, Seconds(frequency))

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> bootstrap_servers_config,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> group_id,
      "security.protocol" -> protocol,
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean))

    val dstream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams))

    val lines = dstream.map(record => (record.key, record.value, record.offset(), record.timestamp()))

    //UDFs
    def current_time = udf(() => { java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")).trim })
    def current_date = udf(() => { java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) })
    def key_gen = udf(() => { Generators.timeBasedGenerator().generate().toString() })

    lines.foreachRDD { rdd =>
      {

        // Get the singleton instance of SparkSession
        val spark = SparkSession.builder.config(rdd.sparkContext.getConf).getOrCreate()
        import spark.implicits._

        // Convert RDD[String, String] to DataFrame
        var df = rdd.toDF("key", "value", "offset", "timestamp")

        if (!df.rdd.isEmpty) {

          val inputDF = df.withColumn("msg_id", concat('timestamp, lit("_"), 'offset)).withColumn("batchDate", current_date())
            .selectExpr("msg_id", "value", "batchDate")

          // Stream-1 : Storing rawDF at HDFS.
          val encodedRawDF = inputDF.withColumn("value", base64(col("value")))
          encodedRawDF.write.partitionBy("batchDate").mode("append").format("csv").option("header", true).save(auditRawPath)

          val parsedDF = inputDF.map(msg => FHIRParserWrapper.parseFHIR(msg, spark))
            .select(
              '_1.as("msg_id"),
              explode('_2.as("value")),
              '_3.as("status"),
              '_4.as("description"))
            .withColumn("batchTime", current_time()).withColumnRenamed("key", "entity")

          val cleanDF = parsedDF.filter('status === "Success")
          cleanDF.show

          // Stream-2 : Storing the parsed dataframe in Text format
          cleanDF.drop("msg_id", "status", "description").write
            .partitionBy("entity", "batchTime")
            .option("header", "false")
            .mode("append").format("text").save(out)

          // Stream-3 : Audit Log the operation - can be stored in db or HDFS batchwise
          val auditLogDF = parsedDF.drop("value")
          auditLogDF.write.mode("append").format("csv").option("header", true).save(auditLogPath)

        }

      }

    }

    ssc.start() // Start the computation
    ssc.awaitTermination()

  }

}