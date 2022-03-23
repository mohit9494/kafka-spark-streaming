package com.ct.kafka.utils

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import java.util.Properties

object PropertyFileReader {

  private var propertyConfiguration: Config = _

  private def readDataReconProperties(propFileName: String) = {
    val configFile = propFileName
    propertyConfiguration = Option(getClass.getClassLoader.getResource(configFile)).fold {
      println(s"ConfigFile ==> $configFile not found !!!! ")
      ConfigFactory.load()
    } { resource =>
      ConfigFactory.load(configFile)
    }

  }

  def getPropertyString(propertyKey: String, propFileName: String): String = {

    propertyConfiguration match {
      case null => {
        readDataReconProperties(propFileName)
        propertyConfiguration.getString(propertyKey)
      }
      case _ => propertyConfiguration.getString(propertyKey)
    }
  }

  case class config(bootstrap_servers_config: String, mysql_url: String, mysql_user: String, mysql_password: String)

  def getPropertyModel(prop: Properties) = {

    //bootstrap_servers_config: String, mysql_url: String, mysql_user: String, mysql_password: String
    val bootstrap_servers_config = prop.getProperty("bootstrap_servers_config").trim
    val mysql_url = prop.getProperty("mysql_url").trim
    val mysql_user = prop.getProperty("mysql_user").trim
    val mysql_password = prop.getProperty("mysql_password").trim

    println("******* bootstrap_servers_config ====> " + bootstrap_servers_config)
    println("******* mysql_url ====> " + mysql_url)
    println("******** mysql_user ====> " + mysql_user)
    println("******** mysql_password ===> " + mysql_password)

    config(bootstrap_servers_config, mysql_url, mysql_user, mysql_password)

  }

}