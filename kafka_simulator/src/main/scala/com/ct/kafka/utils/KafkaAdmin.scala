package com.ct.kafka.utils

import java.util.Arrays
import java.util.Properties

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic

object KafkaAdmin {

  def checkKafkaTopics(jobMetadata: MetadataDAO.JobMetadata, bootStrapServer: String) = {

    val entities = jobMetadata.topic
    val entityList = entities.split(",")

    // val bootstrap_servers_config = PropertyFileReader.getPropertyString("bootstrap_servers_config", "kafka_simulator.properties")
    val config = new Properties()
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer)
    val kafkaAdmin = AdminClient.create(config)

    val partitions = 1
    val replication = 1.toShort

    // Getting list of kafka-topics
    val listTopics = kafkaAdmin.listTopics()
    val kafkaTopics = listTopics.names().get.toArray.map(x => x.toString())

    val requiredTopics = entityList.diff(kafkaTopics)

    if (requiredTopics.isEmpty) {

      println("****** All the required topics are present *****")
    } else {

      requiredTopics.foreach { entity =>
        println("Creating topic ==> " + entity)

        val topic = new NewTopic(entity, partitions, replication)
        val topics = Arrays.asList(topic)

        val topicStatus = kafkaAdmin.createTopics(topics).values()

        println(topicStatus.keySet())
      }

      kafkaAdmin.close()
    }

  }

}