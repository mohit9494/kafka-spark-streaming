package com.ct.spark.dao

import com.ct.spark.model.AuditClass
import java.sql.PreparedStatement
import java.sql.ResultSet
import com.ct.spark.utils.SQLConfig
import java.sql.SQLException
import org.slf4j.LoggerFactory
import com.ct.spark.utils.PropertyFileReader

object MetadataDAO {

  val LOGGER = LoggerFactory.getLogger(MetadataDAO.getClass)

  def getMetadataDetails(jobID: String) = {

    println("*** Inside getMetadataDetails Method ****")

    var stmt: PreparedStatement = null
    var rs: ResultSet = null
    var metadataObj: AuditClass.JobMetadata = null

    val con = SQLConfig.getConnectionURL

    val metaQuery = PropertyFileReader.getPropertyString("metaQuery", "fhir-consumer.properties").trim

    try {

      stmt = con.prepareStatement(metaQuery)
      stmt.setString(1, jobID)
      println("MetaDataQuery ====> " + stmt.toString())
      rs = stmt.executeQuery()

      while (rs.next()) {

        val jobID = rs.getString("job_id").trim()
        val topic = rs.getString("topic").trim()
        val destinationDir = rs.getString("destination_dir").trim()

        metadataObj = AuditClass.JobMetadata(jobID, topic, destinationDir)

      }

      metadataObj

    } catch {

      case se: SQLException => {
        LOGGER.error("SQL Exception occured in  getMetadataDetails() method : " + se.getMessage)
        throw new Exception("SQL Exception occured in  getMetadataDetails() method : " + se.getMessage, se.getCause)
      }
      case e: Exception => {
        LOGGER.error("Exception Occured in  getMetadataDetails() method : " + e.getMessage)
        throw new Exception("Exception occured in  getMetadataDetails() method : " + e.getMessage, e.getCause)

      }

    } finally {

      LOGGER.info("**** Closing Stmt, Connection and ResultSet ****")
      if (stmt != null) stmt.close
      if (con != null) con.close
      if (rs != null) rs.close

    }

  }

}