package com.github.wongelz.guidebook

import org.scalatest.selenium.{Driver, WebBrowser}

trait SemanticQueries { this: WebBrowser with Driver =>

  def buttonText(text: String): Query = xpath(s"//button[contains(.,'$text')]")

  def labelText(text: String): Query = xpath(s"//label[contains(.,'$text')]")

}
