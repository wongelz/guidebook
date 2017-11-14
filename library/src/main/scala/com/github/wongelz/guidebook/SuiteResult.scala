package com.github.wongelz.guidebook

import java.time.Instant

import org.scalatest.events._

import scala.annotation.tailrec

case class SuiteResult(
    suiteId: String,
    suiteName: String,
    suiteClassName: Option[String],
    timestamp: Instant,
    duration: Option[Long],
    browserResult: List[BrowserSuiteResult],
    isCompleted: Boolean) {

  def browsers: List[String] =
    browserResult.map(_.browserName).distinct

  def getJourneys(browserName: String): Option[BrowserSuiteResult] = {
    browserResult.find(_.browserName == browserName)
  }
}

case class BrowserSuiteResult(
    browserName: String,
    timestamp: Instant,
    duration: Long,
    journeys: List[Journey]
)

object SuiteResult {
  def apply(suiteStarting: SuiteStarting, suiteCompleted: SuiteCompleted, suiteEvents: List[Event]): SuiteResult = {
    val browserJourneys = getBrowserJourneys(suiteEvents, Nil)
    SuiteResult(suiteStarting.suiteId, suiteStarting.suiteName, suiteStarting.suiteClassName,
      Instant.ofEpochMilli(suiteStarting.timeStamp), suiteCompleted.duration,
      browserJourneys, isCompleted = true)
  }

  def apply(suiteStarting: SuiteStarting, suiteAborted: SuiteAborted, suiteEvents: List[Event]): SuiteResult = {
    val browserJourneys = getBrowserJourneys(suiteEvents, Nil)
    SuiteResult(suiteStarting.suiteId, suiteStarting.suiteName, suiteStarting.suiteClassName,
      Instant.ofEpochMilli(suiteStarting.timeStamp), suiteAborted.duration,
      browserJourneys, isCompleted = false)
  }

  @tailrec
  private def getBrowserJourneys(events: List[Event], accum: List[BrowserSuiteResult]): List[BrowserSuiteResult] = {
    events match {
      case ScopeOpened(_, openText, _, _, _, _, _, startTimestamp) :: es =>
        val browserRegex = "Using (.+): ".r
        Option(browserRegex.findAllIn(openText).group(1)) match {
          case Some(browserName) =>
            val browserCompleteIdx = es.indexWhere {
              case ScopeClosed(_, closeText, _, _, _, _, _, _) if openText == closeText => true
              case _ => false
            }
            val (browserEvents, bc :: otherEvents) = es.splitAt(browserCompleteIdx)
            bc match {
              case ScopeClosed(_, _, _, _, _, _, _, endTimestamp) =>
                val browserJourneys = BrowserSuiteResult(browserName,
                  Instant.ofEpochMilli(startTimestamp),
                  endTimestamp - startTimestamp,
                  getJourneys(browserEvents, Nil, Nil, Nil, Nil))
                getBrowserJourneys(otherEvents, browserJourneys :: accum)
              case _ => throw new IllegalStateException(s"Expecting ScopeClosed for $openText")
            }
          case _ =>
            getBrowserJourneys(es, accum)
        }
      case _ :: es => getBrowserJourneys(es, accum)
      case Nil => accum
    }
  }

  @tailrec
  private def getJourneys(events: List[Event], scope: List[String], alerts: List[String], notes: List[String], accum: List[Journey]): List[Journey] = {
    events match {
      case Nil => accum.reverse
      case ScopeOpened(_, s, _, formatter, _, _, _, _) :: es =>
        getJourneys(es, getCaption(formatter, s, scope) :: scope, alerts, notes, accum)
      case ScopeClosed(_, _, _, _, _, _, _, _) :: es =>
        getJourneys(es, scope.tail, alerts, notes, accum)
      case TestSucceeded(_, _, suiteId, _, testName, testText, _, _, formatter, _, _, _, _, _) :: es =>
        val step = Step(suiteId, testName, getCaption(formatter, testText, scope), Result.Passed, None, alerts, notes)
        getJourneys(es, scope, Nil, Nil, addStep(step, scope, accum))
      case TestFailed(_, _, _, suiteId, _, testName, testText, _, throwable, _, formatter, _, _, _, _, _) :: es =>
        val step = Step(suiteId, testName, getCaption(formatter, testText, scope), Result.Failed, throwable, alerts, notes)
        getJourneys(es, scope, Nil, Nil, addStep(step, scope, accum))
      case TestCanceled(_, _, _, suiteId, _, testName, testText, _, throwable, _, formatter, _, _, _, _, _) :: es =>
        val step = Step(suiteId, testName, getCaption(formatter, testText, scope), Result.Canceled, throwable, alerts, notes)
        getJourneys(es, scope, Nil, Nil, addStep(step, scope, accum))
      case TestIgnored(_, _, suiteId, _, testName, testText, formatter, _, _, _, _) :: es =>
        val step = Step(suiteId, testName, getCaption(formatter, testText, scope), Result.Ignored, None, alerts, notes)
        getJourneys(es, scope, Nil, Nil, addStep(step, scope, accum))
      case AlertProvided(_, message, _, _, _, _, _, _, _) :: es =>
        getJourneys(es, scope, message :: alerts, notes, accum)
      case NoteProvided(_, message, _, _, _, _, _, _, _) :: es =>
        getJourneys(es, scope, alerts, message :: notes, accum)
      case _ :: es =>
        getJourneys(es, scope, alerts, notes, accum)
    }
  }

  private def getCaption(formatter: Option[Formatter], testText: String, scope: List[String]) = {
    formatter match {
      case Some(IndentedText(_, rawText, _)) =>
        // remove the 'when' word from the browser scope
        if (scope.isEmpty) rawText.replaceFirst("when ", "") else rawText
      case _ => testText
    }
  }

  private def addStep(step: Step, scope: List[String], journeys: List[Journey]): List[Journey] = {
    journeys match {
      case Nil =>
        Journey(scope, step :: Nil) :: Nil
      case j :: js if j.scope == scope =>
        Journey(scope, j.steps :+ step) :: js
      case js =>
        Journey(scope, step :: Nil) :: js
    }
  }

}