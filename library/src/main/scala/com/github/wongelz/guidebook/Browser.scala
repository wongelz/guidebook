package com.github.wongelz.guidebook

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.safari.SafariDriver

abstract class Browser(val name: String) {
  def createWebDriver(): WebDriver
}

/**
  * Some default browsers, without any custom capabilities.
  */
object Browsers {
  object Firefox extends Browser("Firefox") {
    override def createWebDriver(): WebDriver = new FirefoxDriver()
  }

  object Chrome extends Browser("Chrome") {
    override def createWebDriver(): WebDriver = new ChromeDriver()
  }

  object InternetExplorer extends Browser("Internet Explorer") {
    override def createWebDriver(): WebDriver = new InternetExplorerDriver()
  }

  object Safari extends Browser("Safari") {
    override def createWebDriver(): WebDriver = new SafariDriver()
  }

  val AllSupported = Set(Firefox, Chrome, InternetExplorer, Safari)
}

