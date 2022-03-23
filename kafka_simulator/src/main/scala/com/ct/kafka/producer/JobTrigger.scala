package com.ct.kafka.producer

import com.fasterxml.jackson.databind.ObjectMapper

import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer
import com.fasterxml.jackson.databind.JsonNode
import org.apache.kafka.clients.producer.ProducerRecord
import com.ct.kafka.utils.PropertyFileReader
import com.ct.kafka.utils.KafkaAdmin
import com.ct.kafka.utils.MetadataDAO
import java.io.FileInputStream

object JobTrigger {

  def main(args: Array[String]): Unit = {

    println("********* Starting Kafka Producer Job *********")

    val jobID = args(0)

    val prop = new Properties()
    val fileStream = new FileInputStream(args(1).trim)
    prop.load(fileStream)
    fileStream.close

    // Setting Config Attributes
    val configModel = PropertyFileReader.getPropertyModel(prop)

    val jobMetadata = MetadataDAO.getMetadataDetails(jobID, configModel)
    val bootStrapServer = configModel.bootstrap_servers_config

    println("JobId ===> " + jobMetadata.jobID)
    println("topic ===> " + jobMetadata.topic)
    println("sourceDir ===> " + jobMetadata.sourceDir)
    println("bootstrap_server ===> " + bootStrapServer)

    // Checking Kafka Topics
    if (prop.getProperty("kafka_admin_enable").equalsIgnoreCase("true")) {
      KafkaAdmin.checkKafkaTopics(jobMetadata, bootStrapServer)
    }

    // Sending records from Kafka Producer
    KafkaRecordProducer.recordProducer(jobMetadata, bootStrapServer, prop)

    println("*** Kafka Producer Sent Records for all the Entities *****")

  }

}