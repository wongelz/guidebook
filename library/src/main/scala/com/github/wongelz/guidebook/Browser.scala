package com.github.wongelz.guidebook

import java.io.File

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions}
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.safari.SafariDriver

/**
  * Base class for a browser. Consists of a name and WebDriver factory method.
  * @param name unique name per test project for Guidebook to identify the browser (eg. cannot have multiple 'Chrome')
  */
abstract class Browser(val name: String) {

  /**
    * Create a new instance of WebDriver.
    */
  def createWebDriver(): WebDriver
}

/**
  * Some default browsers, without any custom capabilities.
  */
object Browsers {
  final case class Firefox(headless: Boolean) extends Browser("Firefox") with Detectable {
    override def createWebDriver(): WebDriver = {
      val options = new FirefoxOptions()
      options.setHeadless(headless)
      new FirefoxDriver(options)
    }

    override def isPresent(): Boolean =
      Option(System.getProperty("webdriver.gecko.driver")).exists(fileExists)
  }

  final case class Chrome(headless: Boolean) extends Browser("Chrome") with Detectable {
    override def createWebDriver(): WebDriver = {
      val options = new ChromeOptions
      options.setHeadless(headless)
      new ChromeDriver(options)
    }

    override def isPresent(): Boolean =
      Option(System.getProperty("webdriver.chrome.driver")).exists(fileExists)
  }

  object InternetExplorer extends Browser("Internet Explorer") with Detectable {
    override def createWebDriver(): WebDriver = new InternetExplorerDriver()

    override def isPresent(): Boolean =
      Option(System.getProperty("webdriver.ie.driver")).exists(fileExists)
  }

  object Edge extends Browser("Edge") with Detectable {
    override def createWebDriver(): WebDriver = new EdgeDriver()

    override def isPresent(): Boolean =
      Option(System.getProperty("webdriver.edge.driver")).exists(fileExists)
  }

  object Safari extends Browser("Safari") with Detectable {
    override def createWebDriver(): WebDriver = new SafariDriver()

    override def isPresent(): Boolean =
      fileExists("/usr/bin/safaridriver")
  }

  private def fileExists(pathname: String) =
    new File(pathname).exists()

  val Detectable: Set[Detectable] = Set(Firefox(headless = false), Chrome(headless = false), InternetExplorer, Edge, Safari)
}

