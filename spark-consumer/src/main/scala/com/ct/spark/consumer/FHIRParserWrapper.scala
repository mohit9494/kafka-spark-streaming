package com.ct.spark.consumer

import com.citiustech.fhir.parser._
import org.apache.spark.sql.{ Row, SparkSession }

import scala.collection.JavaConversions._
import com.ct.spark.utils.FHIRUtils

object FHIRParserWrapper {

  def parseFHIR(msg: Row, spark: SparkSession) = {

    println("******** Calling FHIR Parser *********")

    val msg_id = msg.getAs[String]("msg_id")
    val message = msg.getAs[String]("value")

    // TODO - Pass the UUID to this parser - Adding msg_id directly to the message
    // TODO - If the message is malformed then should not throw an error return proper failure response - Pending
    val response = Parser.builder.build.parse(message)

    // Call to responce checker to determine status whether Success or Fail
    val parserResponce = ResponseChecker.checkResponse(response)

    val status = parserResponce._1
    val description = parserResponce._2
    val uuid = parserResponce._3
    var entityMap = scala.collection.mutable.Map[String, String]()

    if (status.equalsIgnoreCase("Success")) {

      var entityList = response.iterator.toList.flatMap(x => x.records).map(x => (x._1.trim, FHIRUtils.getRestructuredMessage(x, msg_id, uuid)))

      entityMap = entityMap.++(entityList.groupBy(_._1).map { case (k, v) => (k, v.map(_._2).mkString("[", ",", "]")) })

    } else {

      println("******** Bad Resources Observed ***********")
      val badResourceType = FHIRUtils.getBadResourceType(message)
      entityMap += (badResourceType -> message)
    }

    (msg_id, entityMap, status, description)

  }

}