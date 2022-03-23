package com.ct.spark.consumer

import com.citiustech.fhir.parser.Response

object ResponseChecker {

  def checkResponse(responce: Response) = {

    var status: String = null
    var description: String = null

    val badRecords = responce.badRecords
    val errors = responce.errors
    val unprocessedRecords = responce.unprocessedRecords
    val uuid = responce.uuid

    if (errors.length == 0 && badRecords.length == 0) {

      status = "Success"
      description = s"errors = ${errors.length}, bad records = ${badRecords.length}, unprocessed records = ${unprocessedRecords.length}"

    } else {
      status = "Failure"
      description = s"errors = ${errors.length}, bad records = ${badRecords.length}, unprocessed records = ${unprocessedRecords.length}, Error Messages = ${errors.mkString(", ")}"
    }

    (status, description, uuid)

  }

}