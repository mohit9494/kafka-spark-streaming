package com.ct.spark.consumer

import org.apache.spark.sql.SparkSession

import com.ct.spark.model.AuditClass
import com.ct.spark.utils.PropertyFileReader

object SparkConsumer {

  def recordConsumer(spark: SparkSession, jobMetadata: AuditClass.JobMetadata): Unit = {

    val source_type = PropertyFileReader.getPropertyString("source_type", "fhir-consumer.properties").trim.toLowerCase

    println("Source_Type ====> " + source_type)

    source_type.toLowerCase match {

      case "kafka" => kafkaRecordConsumer.kafkaRecordConsumer(spark, jobMetadata)
      case "hdfs"  => socketRecordConsumer.hdfsRecordConsumer(spark, jobMetadata)
      case "dstream"  => DStream.dstreamConsumer(spark, jobMetadata)
      case _       => println("Invalid Source Type. Allowed Source Types ==> kafka | hdfs | dstream ")

    }

  }

}