package com.github.wongelz.guidebook

import java.util

import com.github.wongelz.guidebook.selenium.WebBrowserExtras
import org.openqa.selenium.{WebDriver, WebDriverException}
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.WebBrowser
import org.scalatest.time.{Millis, Seconds, Span}

trait Guidebook extends WordSpec
  with MustMatchers
  with Eventually
  with TestSuiteMixin
  with WebBrowser
  with WebBrowserExtras
  with BeforeAndAfterAll {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(3000, Millis)), interval = scaled(Span(100, Millis)))

  private var browsers: List[Browser] = List.empty
  private var currentWebDriver: Option[WebDriver] = None
  private var currentBrowser: Option[Browser] = None

  @volatile private var cancelRemaining = false

  implicit def webDriver: WebDriver =
    synchronized {
      currentWebDriver.get
    }

  implicit def browser: Browser =
    synchronized {
      currentBrowser.get
    }

  /**
    * Registers suites/tests to run using one or more browsers.
    *
    * eg.
    * usingBrowsers(Chrome, Firefox) {
    *    ...
    * }
    */
  def usingBrowsers(browser: Browser, browsers: Browser*)(tests: => Unit): Unit = {
    this.browsers = browser :: browsers.toList
    for (browser <- this.browsers) {
      s"Using ${browser.name}: " when {
        tests
      }
    }
  }

  /**
    * Registers suites/tests to run using one or more browsers, providing to access the browser name.
    *
    * eg.
    * foreachBrowser(Chrome, Firefox) { browser =>
    *    ...
    * }
    */
  def foreachBrowser(browser: Browser, browsers: Browser*)(tests: String => Unit): Unit = {
    this.browsers = browser :: browsers.toList
    for (browser <- this.browsers) {
      s"Using ${browser.name}: " when {
        tests(browser.name)
      }
    }
  }

  /**
    * Registers suites/tests to run using a given browser.
    *
    * eg.
    * usingBrowser(Chrome) {
    *    ...
    * }
    */
  def usingBrowser(browser: Browser)(tests: => Unit): Unit =
    usingBrowsers(browser)(tests)

  /**
    * Registers suites/tests to run using all detected browsers.
    *
    * eg.
    * usingDetectedBrowsers { browser =>
    *    ...
    * }
    */
  def usingDetectedBrowsers(tests: => Unit): Unit = {
    Browsers.Detectable.toList.filter(_.isPresent()) match {
      case b :: bs => usingBrowsers(b, bs:_*)(tests)
      case Nil => throw new WebDriverException("Unable to detect any browsers")
    }
  }

  /**
    * Registers suites/tests to run using all detected browsers, providing access to the browser name.
    *
    * eg.
    * foreachDetectedBrowser { browser =>
    *    ...
    * }
    */
  def foreachDetectedBrowser(tests: String => Unit): Unit = {
    Browsers.Detectable.toList.filter(_.isPresent()) match {
      case b :: bs => foreachBrowser(b, bs:_*)(tests)
      case Nil => throw new WebDriverException("Unable to detect any browsers")
    }
  }

  abstract override def withFixture(test: NoArgTest): Outcome = {
    browsers.find(b => test.name.startsWith(s"Using ${b.name}:")) match {
      case Some(b) if currentBrowser.exists(_.name == b.name) =>
      // re-use the current browser/driver
      case Some(b) =>
        synchronized {
          cancelRemaining = false
          currentWebDriver foreach (_.quit())
          currentBrowser = Some(b)
          currentWebDriver = Some(b.createWebDriver())
        }
      case None =>
        synchronized {
          currentWebDriver foreach (_.quit())
          currentBrowser = None
          currentWebDriver = None
        }
    }

    if (cancelRemaining) {
      Canceled("Canceled by Guidebook because a test failed previously")
    } else {
      implicitlyWait(Span(0, Seconds))
      setCaptureDir("target/guidebook/screenshots")
      webDriver.manage().window().setPosition(new org.openqa.selenium.Point(0, 0))
      val screens = Screens.All
      resizeViewport(screens.head)

      val outcome = super.withFixture(test)

      val stepId = Step.id(suiteId, test.name)
      capture to s"$stepId.png"
      for (s <- screens.tail) {
        resizeViewport(s)
        capture to s"$stepId${s.suffix}.png"
      }

      outcome match {
        case failed: Failed =>
          cancelRemaining = true
          failed
        case _ => outcome
      }
    }
  }

  override def afterAll(): Unit = {
    currentWebDriver foreach (_.quit())
    super.afterAll()
  }

  private def resizeViewport(screen: Screen): Unit = {
    val browserPadding = executeScript("""
      return [window.outerWidth - window.innerWidth,
              window.outerHeight - window.innerHeight];
    """).asInstanceOf[util.List[Long]]
    webDriver.manage().window().setSize(new org.openqa.selenium.Dimension(
      screen.width + browserPadding.get(0).toInt,
      screen.height + browserPadding.get(1).toInt))
  }
}
