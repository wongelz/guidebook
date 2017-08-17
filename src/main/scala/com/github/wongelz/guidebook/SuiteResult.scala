package com.github.wongelz.guidebook

import org.scalatest.events._

import scala.annotation.tailrec

case class SuiteResult(
    suiteId: String,
    suiteName: String,
    suiteClassName: Option[String],
    duration: Option[Long],
    startEvent: SuiteStarting,
    endEvent: Event,
    eventList: collection.immutable.IndexedSeq[Event],
    testsSucceededCount: Int,
    testsFailedCount: Int,
    testsIgnoredCount: Int,
    testsPendingCount: Int,
    testsCanceledCount: Int,
    scopesPendingCount: Int,
    isCompleted: Boolean) {

  def journeys: List[Journey] =
    getJourneys(eventList.toList, Nil, Nil, Nil, Nil).reverse

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
