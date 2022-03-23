package com.ct.spark.consumer

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{ DataType, StructType }
import org.apache.spark.sql.types._
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.spark.sql.functions.split
import org.apache.spark.sql.Row
import java.time.format.DateTimeFormatter
import org.apache.spark.sql.DataFrame
import com.ct.spark.utils.PropertyFileReader
import java.time.ZoneOffset
import java.sql.DriverManager
import java.sql.Connection
import java.sql.Statement
import java.sql.PreparedStatement
import com.ct.spark.model.AuditClass
import com.fasterxml.uuid.Generators

object socketRecordConsumer {

  def hdfsRecordConsumer(spark: SparkSession, jobMetadata: AuditClass.JobMetadata) = {

    println("******* Inside hdfsRecordConsumer() **********")

    val streaming_dir = PropertyFileReader.getPropertyString("streaming_dir", "fhir-consumer.properties").trim

    val out = jobMetadata.destinationDir

    println("*** streaming_dir ===> " + streaming_dir)

    def current_time = udf(() => { java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")).trim })
    def current_date = udf(() => { java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) })
    def key_gen = udf(() => { Generators.timeBasedGenerator().generate().toString() })

    import spark.implicits._

    val inputStream = spark.readStream.text(streaming_dir)
      .withColumn("msg_id", key_gen())
      .withColumn("topic", lit(jobMetadata.topic))

    // RawDF - Deserialized => cols(msg_id, value, batchDate)
    val rawDF = inputStream.selectExpr("msg_id", "value", "topic")
      .as[(String, String, String)]
      .withColumn("batchDate", current_date())
      .toDF()

    // Stream-1 : Call to audit consumer to store rawDF at HDFS
    AuditConsumer.auditMessage(rawDF)

    // Calling FHIR Parser for Parsing
    // parsedDF => cols(msg_id, entity(resourceType), value, status, desription, batchTime)
    val parsedDF = rawDF.map(msg => FHIRParserWrapper.parseFHIR(msg, spark))
      .select(
        '_1.as("msg_id"),
        explode('_2.as("value")),
        '_3.as("status"),
        '_4.as("description"))
      .withColumn("batchTime", current_time()).withColumnRenamed("key", "entity")

    val cleanDF = parsedDF.filter('status === "Success")

    // Stream-2 : Storing the parsed dataframe in Text format
    cleanDF.drop("msg_id", "status", "description")
      .writeStream.partitionBy("entity", "batchTime")
      .outputMode("append")
      .format("text")
      .option("header", "false")
      .option("checkpointLocation", s"$out/checkpoint")
      .option("path", s"$out/data")
      .start()

    // Stream-3 : Audit Log the operation - can be stored in db or HDFS batchwise
    AuditConsumer.auditLog(parsedDF)

    // Printing on console
    cleanDF.writeStream.outputMode("append").format("console").start()

    // Spark StreamingQueryManager to manage all the streams
    spark.streams.awaitAnyTermination()

  }

}