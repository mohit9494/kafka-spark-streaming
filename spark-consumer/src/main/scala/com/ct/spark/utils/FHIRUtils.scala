package com.ct.spark.utils

import scala.util.parsing.json.JSON
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.apache.spark.sql.SparkSession
import com.ct.spark.model.AuditClass
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.spark.sql.DataFrame

object FHIRUtils {

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)

  def getRestructuredMessage(responce: (String, String), msg_id: String, uuid: String) = {

    val entity = responce._1.trim
    val msg = responce._2

    val parsed = JSON.parseFull(msg).get.asInstanceOf[Map[String, String]]
    var parsedMap = collection.mutable.LinkedHashMap(parsed.toSeq: _*)

    if (entity.equalsIgnoreCase("documentReferenceContent_spec")) {
      val decodedData = new String(Base64.getDecoder.decode(parsedMap.getOrElse("data", "")), StandardCharsets.UTF_8)
      parsedMap += ("data" -> decodedData)
    }

    parsedMap += ("msg_id" -> msg_id)
    parsedMap += ("uuid" -> uuid)

    val restructuredMsg = mapper.writeValueAsString(parsedMap)
    restructuredMsg
  }

  def getBadResourceType(msg: String) = {

    val parsed = JSON.parseFull(msg).get.asInstanceOf[Map[String, String]]

    val badResourceType = parsed.get("resourceType").getOrElse("bad_resource").toLowerCase

    badResourceType

  }

  def getInputStream(spark: SparkSession, jobMetadata: AuditClass.JobMetadata) = {

    val krb_enabled = PropertyFileReader.getPropertyString("krb_enabled", "fhir-consumer.properties").trim
    val bootstrap_servers_config = PropertyFileReader.getPropertyString("bootstrap_servers_config", "fhir-consumer.properties").trim
    val topics = jobMetadata.topic

    println("krb_enabled ===> " + krb_enabled)
    println("*** Topics ===> " + topics)
    println("*** bootstrap_servers_config ===> " + bootstrap_servers_config)

    var inputStream: DataFrame = null

    if (krb_enabled.equalsIgnoreCase("true")) {

      val group_id = PropertyFileReader.getPropertyString("group_id", "fhir-consumer.properties").trim
      val protocol = PropertyFileReader.getPropertyString("protocol", "fhir-consumer.properties").trim
      val service_name = PropertyFileReader.getPropertyString("service_name", "fhir-consumer.properties").trim

      println("*** group_id ===> " + group_id)
      println("*** protocol ===> " + protocol)
      println("*** service_name ===> " + service_name)

      inputStream = spark.readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", bootstrap_servers_config)
        .option("subscribe", topics)
        .option("spark.streaming.backpressure.enabled", "true")
        .option("failOnDataLoss", "false")
        .option(ConsumerConfig.GROUP_ID_CONFIG, group_id)
        .option("kafka.security.protocol", protocol)
        .option("sasl.kerberos.service.name", service_name)
        .load()

    } else {

      inputStream = spark.readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", bootstrap_servers_config)
        .option("subscribe", topics)
        .option("spark.streaming.backpressure.enabled", "true")
        .option("failOnDataLoss", "false")
        .load()

    }

    inputStream

  }

}