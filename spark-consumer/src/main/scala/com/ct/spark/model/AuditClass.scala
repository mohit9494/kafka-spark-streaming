package com.ct.spark.model

object AuditClass {

  case class JobMetadata(jobID:String, topic:String, destinationDir:String)

}