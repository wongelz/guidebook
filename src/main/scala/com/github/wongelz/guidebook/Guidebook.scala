package com.github.wongelz.guidebook

import java.util

import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.selenium.{Driver, WebBrowser}
import org.scalatest.time.{Millis, Seconds, Span}

trait Guidebook extends WordSpec
    with MustMatchers
    with Eventually
    with BeforeAndAfterAll
    with SemanticQueries
    with CancelAfterFailure
    with WebBrowser { this: WebBrowser with Driver =>

  override implicit val patienceConfig = PatienceConfig(timeout = scaled(Span(2000, Millis)), interval = scaled(Span(100, Millis)))

  abstract override def run(testName: Option[String], args: Args): Status = {
    implicitlyWait(Span(0, Seconds))
    setCaptureDir("target/guidebook/screenshots")
    webDriver.manage().window().setPosition(new org.openqa.selenium.Point(0, 0))

    super.run(testName, args)
  }

  protected override def runTest(testName: String, args: Args): Status = {
    val screens = Screens.All
    resizeViewport(screens.head)

    val status = super.runTest(testName, args)

    val stepId = StepId(suiteId, testName)
    capture to s"${stepId.hash}.png"
    for (s <- screens.tail) {
      resizeViewport(s)
      capture to s"${stepId.hash}${s.suffix}.png"
    }
    status
  }

  override def afterAll() = {
    windowHandles foreach { handle =>
      switch to window(handle)
      close()
    }
  }

  private def resizeViewport(screen: Screen) = {
    val browserPadding = executeScript("""
      return [window.outerWidth - window.innerWidth,
              window.outerHeight - window.innerHeight];
    """).asInstanceOf[util.List[Long]]
    webDriver.manage().window().setSize(new org.openqa.selenium.Dimension(
      screen.width + browserPadding.get(0).toInt,
      screen.height + browserPadding.get(1).toInt))
  }
}
