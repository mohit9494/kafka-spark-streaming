package com.ct.spark.utils

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

import org.slf4j.LoggerFactory
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor

object SQLConfig {

  val LOGGER = LoggerFactory.getLogger(SQLConfig.getClass)

  def getConnectionURL() = {

    var conn: Connection = null

    try {

      val url = PropertyFileReader.getPropertyString("mysql_url", "fhir-consumer.properties").trim
      val user = PropertyFileReader.getPropertyString("mysql_user", "fhir-consumer.properties").trim
      val secret_key = PropertyFileReader.getPropertyString("secret_key", "fhir-consumer.properties").trim

      val password = getDecryptedPassword(secret_key)

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

  def getDecryptedPassword(secretKey: String) = {

    val encryptedPassword = PropertyFileReader.getPropertyString("mysql_password", "fhir-consumer.properties").trim

    val encryptor = new StandardPBEStringEncryptor()
    encryptor.setPassword(secretKey)
    encryptor.decrypt(encryptedPassword)

  }

}