package com.ct.kafka.producer

import com.fasterxml.jackson.databind.ObjectMapper

import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer
import com.fasterxml.jackson.databind.JsonNode
import org.apache.kafka.clients.producer.ProducerRecord
import com.ct.kafka.utils.PropertyFileReader
import com.fasterxml.uuid.Generators
import com.ct.kafka.utils.MetadataDAO
import java.io.File

object KafkaRecordProducer {

  def recordProducer(jobMetadata: MetadataDAO.JobMetadata, bootStrapServer: String, prop: Properties) = {

    println("******* Inside Record Producer ***********")

    val authFlag = prop.getProperty("krb_enabled").trim
    println("Krb_Auth ====> " + authFlag)

    val mapper = new ObjectMapper()
    val props = new Properties()
    props.put("bootstrap.servers", bootStrapServer)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.connect.json.JsonSerializer")
    props.put("acks", "all")
   

    if (authFlag.equalsIgnoreCase("true")) {
      
      val service_name = prop.getProperty("service_name").trim
      val protocol = prop.getProperty("protocol").trim
      println("service_name and protocol ===> " + service_name, protocol)

      props.put("security.protocol", protocol)
      props.put("sasl.kerberos.service.name", service_name)
    }

    println("******* Inside Record Producer after propertes File setup ***********")
    val producer = new KafkaProducer[String, JsonNode](props)

    val files = new File(jobMetadata.sourceDir).listFiles()
    val topic = jobMetadata.topic

    files.foreach { file =>
      println("**** Reading File ******* " + file)
      val node = mapper.readTree(file)

      val record = new ProducerRecord[String, JsonNode](topic, node)
      val metadata = producer.send(record)

      println(s"Sent the record ==> key=${record.key}, partition=${metadata.get.partition}, offset=${metadata.get.offset}")

    }

    producer.close()

  }

}