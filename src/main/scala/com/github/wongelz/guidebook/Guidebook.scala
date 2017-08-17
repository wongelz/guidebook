package com.github.wongelz.guidebook

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
    webDriver.manage().window().maximize()

    super.run(testName, args)
  }

  protected override def runTest(testName: String, args: Args): Status = {
    val status = super.runTest(testName, args)
    val stepId = StepId(suiteId, testName)
    capture to s"${stepId.hash}.png"
    status
  }

  override def afterAll() = {
    windowHandles foreach { handle =>
      switch to window(handle)
      close()
    }
  }
}
