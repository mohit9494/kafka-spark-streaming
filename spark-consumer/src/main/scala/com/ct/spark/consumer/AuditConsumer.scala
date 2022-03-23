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
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.streaming.StreamingQueryListener
import org.apache.spark.sql.streaming.StreamingQueryListener.QueryStartedEvent
import org.apache.spark.sql.streaming.StreamingQueryListener.QueryTerminatedEvent
import org.apache.spark.sql.streaming.StreamingQueryListener.QueryProgressEvent
import java.time.ZoneOffset
import java.sql.DriverManager
import java.sql.Connection
import java.sql.Statement
import java.sql.PreparedStatement

object AuditConsumer {

  def auditMessage(auditRawDF: DataFrame): Unit = {

    // TODO move the path to properties file --> Completed
    val auditRawPath = PropertyFileReader.getPropertyString("audit_raw_path", "fhir-consumer.properties").trim

    // TODO move the path to properties file --> Completed
    // TODO CSV - convert the text in Base64 code - UUID along with it - topics  --> Completed
    // TODO partition only on the current day - remove the topic and msg_id --> Completed

    val encodedRawDF = auditRawDF.withColumn("value", base64(col("value")))

    encodedRawDF.writeStream
      .partitionBy("batchDate")
      .outputMode("append")
      .format("csv")
      .option("header", true)
      .option("checkpointLocation", s"$auditRawPath/checkpoint")
      .option("path", s"$auditRawPath/data")
      .start()

  }

  def auditLog(parsedDF: DataFrame) = {

    val auditLogPath = PropertyFileReader.getPropertyString("audit_log_path", "fhir-consumer.properties").trim

    val auditLogDF = parsedDF.drop("value")

    auditLogDF.writeStream
      .outputMode("append")
      .format("csv")
      .option("header", true)
      .option("checkpointLocation", s"$auditLogPath/checkpoint")
      .option("path", s"$auditLogPath/data")
      .start()

  }

}