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
    journeys: List[Journey],
    isCompleted: Boolean)

object SuiteResult {
  def apply(suiteStarting: SuiteStarting, suiteCompleted: SuiteCompleted, suiteEvents: List[Event]): SuiteResult = {
    SuiteResult(suiteStarting.suiteId, suiteStarting.suiteName, suiteStarting.suiteClassName,
      Instant.ofEpochMilli(suiteStarting.timeStamp), suiteCompleted.duration,
      getJourneys(suiteEvents, Nil, Nil, Nil, Nil).reverse, isCompleted = true)
  }

  def apply(suiteStarting: SuiteStarting, suiteAborted: SuiteAborted, suiteEvents: List[Event]): SuiteResult =
    SuiteResult(suiteStarting.suiteId, suiteStarting.suiteName, suiteStarting.suiteClassName,
      Instant.ofEpochMilli(suiteStarting.timeStamp), suiteAborted.duration,
      getJourneys(suiteEvents, Nil, Nil, Nil, Nil).reverse, isCompleted = false)

  @tailrec
  private def getJourneys(events: List[Event], scope: List[String], alerts: List[String], notes: List[String], accum: List[Journey]): List[Journey] = events match {
    case Nil => accum
    case ScopeOpened(_, s, _, _, _, _, _, _) :: es =>
      getJourneys(es, s :: scope, alerts, notes, accum)
    case ScopeClosed(_, _, _, _, _, _, _, _) :: es =>
      getJourneys(es, scope.tail, alerts, notes, accum)
    case TestSucceeded(_, _, suiteId, _, testName, testText, _, _, formatter, _, _, _, _, _) :: es =>
      val step = Step(StepId(suiteId, testName), getCaption(formatter, testText), Result.Passed, None, None, alerts, notes)
      getJourneys(es, scope, Nil, Nil, addStep(step, scope, accum))
    case TestFailed(_, message, _, suiteId, _, testName, testText, _, throwable, _, formatter, _, _, _, _, _) :: es =>
      val step = Step(StepId(suiteId, testName), getCaption(formatter, testText), Result.Failed, Some(message), throwable, alerts, notes)
      getJourneys(es, scope, Nil, Nil, addStep(step, scope, accum))
    case TestCanceled(_, message, _, suiteId, _, testName, testText, _, throwable, _, formatter, _, _, _, _, _) :: es =>
      val step = Step(StepId(suiteId, testName), getCaption(formatter, testText), Result.Canceled, Some(message), throwable, alerts, notes)
      getJourneys(es, scope, Nil, Nil, addStep(step, scope, accum))
    case TestIgnored(_, _, suiteId, _, testName, testText, formatter, _, _, _, _) :: es =>
      val step = Step(StepId(suiteId, testName), getCaption(formatter, testText), Result.Ignored, None, None, alerts, notes)
      getJourneys(es, scope, Nil, Nil, addStep(step, scope, accum))
    case AlertProvided(_, message, _, _, _, _, _, _, _) :: es =>
      getJourneys(es, scope, message :: alerts, notes, accum)
    case NoteProvided(_, message, _, _, _, _, _, _, _) :: es =>
      getJourneys(es, scope, alerts, message :: notes, accum)
    case _ :: es =>
      getJourneys(es, scope, alerts, notes, accum)
  }

  private def getCaption(formatter: Option[Formatter], testText: String) = {
    formatter match {
      case Some(IndentedText(_, rawText, _)) => rawText
      case _ => testText
    }
  }

  private def addStep(step: Step, scope: List[String], journeys: List[Journey]): List[Journey] = journeys match {
    case Nil =>
      Journey(scope, step :: Nil) :: Nil
    case j :: js if j.scope == scope =>
      Journey(scope, step :: j.steps) :: js
    case js =>
      Journey(scope, step :: Nil) :: js
  }

}