package com.github.wongelz.guidebook.selenium

import org.openqa.selenium.{InvalidElementStateException, WebDriver}
import org.openqa.selenium.support.ui.Select
import org.scalatest.selenium.WebBrowser

trait SemanticQueries { this: WebBrowser =>
  /**
    * Query for button(s) with the given text.
    *
    * This method enables syntax such as the following:
    *
    * click on buttonText("???")
    */
  def buttonText(text: String): Query =
    xpath(s"//button[contains(.,'$text')]")

  /**
    * Query for label(s) with the given text.
    * Form fields can be accessed by clicking on its label.
    *
    * This method enables syntax such as the following:
    *
    * click on labelText("???")
    */
  def labelText(text: String): Query =
    xpath(s"//label[contains(.,'$text')]")

  /**
    * Query for form element with the given label text.
    *
    * This method enables syntax such as the following:
    *
    * textField(labelledAs("???")) value_= "???"
    */
  def labelledAs(text: String)(implicit webDriver: WebDriver): Query = {
    labelText(text).findElement match {
      case None => throw new NoSuchElementException(s"Cannot locate label with text: $text")
      case Some(label) =>
        label.attribute("for") match {
          case None => throw new InvalidElementStateException(s"Label with text: $text does not have 'for' attribute")
          case Some(i) => id(i)
        }
    }
  }
}

trait AriaQueries { this: WebBrowser =>

  /**
    * Query for element(s) with the given aria-label attribute
    *
    * This method enables syntax such as the following:
    *
    * click on ariaLabel("???")
    */
  def ariaLabel(text: String): Query =
    xpath(s"//*[@aria-label='$text']")
}

trait EnhancedElements { this: WebBrowser =>
  implicit class EnhancedSingleSel(singleSel: SingleSel) {
    def visibleText_=(value : String) = {
      new Select(singleSel.underlying).selectByVisibleText(value)
    }
  }
}

trait WebBrowserExtras extends SemanticQueries with AriaQueries with EnhancedElements { this: WebBrowser =>

}
