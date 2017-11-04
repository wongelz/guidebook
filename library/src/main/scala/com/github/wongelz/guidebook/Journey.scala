package com.github.wongelz.guidebook

import java.io.{ByteArrayOutputStream, PrintStream}

import com.github.wongelz.guidebook.Result.Failed
import org.apache.commons.codec.digest.DigestUtils

case class Journey(
    scope: List[String],
    steps: List[Step]) {

  lazy val description: String = scope.reverse.mkString(" ")

  lazy val stepCount: Int = steps.length

  lazy val passedStepCount: Int = steps.count(_.result == Result.Passed)

  lazy val percentComplete: Long = (passedStepCount.toDouble / stepCount * 100).round

  lazy val passed: Boolean = steps.forall(_.result == Result.Passed)
}

case class Step(
    id: String,
    caption: String,
    result: Result,
    stacktrace: Option[String],
    alerts: List[String],
    notes: List[String]) {

  def screenshot(screen: Screen): String =
    s"screenshots/${Step.screenshotFilename(id, screen)}"
}

object Step {

  def apply(suiteId: String, testName: String, caption: String, result: Result, throwable: Option[Throwable],
            alerts: List[String], notes: List[String]): Step =
    Step(id(suiteId, testName), caption, result, if (result == Failed) throwable.map(getStackTrace) else None, alerts, notes)

  def id(suiteId: String, testName: String): String =
    new String(DigestUtils.sha1Hex(s"$suiteId$testName"))

  def screenshotFilename(id: String, screen: Screen) = s"$id-${screen.width}x${screen.height}.png"

  private def getStackTrace(th: Throwable): String = {
    val out = new ByteArrayOutputStream()
    th.printStackTrace(new PrintStream(out))
    new String(out.toByteArray)
  }
}

sealed trait Result
object Result {
  case object Passed extends Result
  case object Failed extends Result
  case object Canceled extends Result
  case object Ignored extends Result
}