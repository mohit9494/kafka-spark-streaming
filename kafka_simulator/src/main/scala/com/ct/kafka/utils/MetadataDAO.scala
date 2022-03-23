package com.ct.kafka.utils

import java.sql.PreparedStatement
import java.sql.ResultSet

import java.sql.SQLException
import org.slf4j.LoggerFactory

object MetadataDAO {

  val LOGGER = LoggerFactory.getLogger(MetadataDAO.getClass)

  case class JobMetadata(jobID: String, topic: String, sourceDir: String)

  def getMetadataDetails(jobID: String, configModel: PropertyFileReader.config) = {

    println("*** Inside getMetadataDetails Method ****")

    var stmt: PreparedStatement = null
    var rs: ResultSet = null
    var metadataObj: JobMetadata = null

    val con = SQLConfig.getConnectionURL(configModel)

    val metaQuery = "select * from producer_metadata where job_id=?".trim

    try {

      stmt = con.prepareStatement(metaQuery)
      stmt.setString(1, jobID)
      println("MetaDataQuery ====> " + stmt.toString())
      rs = stmt.executeQuery()

      while (rs.next()) {

        val jobID = rs.getString("job_id").trim()
        val topic = rs.getString("topic").trim()
        val source_dir = rs.getString("source_dir").trim()

        metadataObj = JobMetadata(jobID, topic, source_dir)

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