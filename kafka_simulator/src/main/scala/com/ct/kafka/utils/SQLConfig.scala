package com.ct.kafka.utils

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

import org.slf4j.LoggerFactory

object SQLConfig {

  val LOGGER = LoggerFactory.getLogger(SQLConfig.getClass)

  def getConnectionURL(configModel: PropertyFileReader.config) = {

    var conn: Connection = null

    try {

      Class.forName("com.mysql.cj.jdbc.Driver")

      val url = configModel.mysql_url
      val user = configModel.mysql_user
      val password = configModel.mysql_password

      conn = DriverManager.getConnection(url, user, password)
    } catch {
      case se: SQLException => {
        LOGGER.error("SQL Exception occured in  getConnectionURL() method : " + se.getMessage)
        throw new Exception("SQL Exception occured in  getConnectionURL() method : " + se.getMessage, se.getCause)
      }
      case e: Exception => {
        LOGGER.error("Exception Occured in  getConnectionURL() method :" + e.getMessage)
        throw new Exception("Exception occured in  getConnectionURL() method : " + e.getMessage, e.getCause)

      }
    }
    conn

  }

}