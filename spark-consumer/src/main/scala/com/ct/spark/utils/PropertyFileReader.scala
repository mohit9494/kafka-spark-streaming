package com.ct.spark.utils

import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config

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

}