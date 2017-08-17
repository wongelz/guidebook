package com.github.wongelz.guidebook

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

case class StepId(suiteId: String, testName: String) {
  def hash = new String(DigestUtils.sha1Hex(s"$suiteId$testName"))
}

case class Step(
    id: StepId,
    caption: String,
    result: Result,
    message: Option[String],
    throwable: Option[Throwable],
    alerts: List[String],
    notes: List[String]) {

  lazy val screenshot = s"screenshots/${id.hash}.png"
}

sealed trait Result
object Result {
  case object Passed extends Result
  case object Failed extends Result
  case object Canceled extends Result
  case object Ignored extends Result
}