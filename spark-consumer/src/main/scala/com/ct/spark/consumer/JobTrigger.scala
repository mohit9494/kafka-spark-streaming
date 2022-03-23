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
import com.ct.spark.dao.MetadataDAO

object JobTrigger {

  def main(args: Array[String]): Unit = {

    System.setProperty("hadoop.home.dir", "C:\\winutils")

    Logger.getLogger("org").setLevel(Level.ERROR)
    println("Consumer Aplication Started")

    // val jobID = args(0).trim
    val jobID = "1"
    println("jobID =====> " + jobID)

    // ToDo - Source Type in Metadata - HDFS , Kafka
    // Common Transformation and Diff Implementation
    //Spark FHIR Parser => Password Encryption

    val spark = SparkSession.builder().master("local[*]").getOrCreate()
    // val spark = SparkSession.builder().getOrCreate()
    val jobMetadata = MetadataDAO.getMetadataDetails(jobID)

    println("JobId ===> " + jobMetadata.jobID)
    println("topic ===> " + jobMetadata.topic)
    println("destinationDir ===> " + jobMetadata.destinationDir)

    // Calling Spark Record Consumer
    SparkConsumer.recordConsumer(spark, jobMetadata)

  }

}