package com.github.wongelz.guidebook

import java.time.{Duration, Instant}

import scala.collection.mutable.ListBuffer

class SuiteResultHolder {

  val suiteList = new ListBuffer[SuiteResult]()

  def +=(result: SuiteResult): Unit = {
    suiteList += result
  }

  def suiteJourneys: List[(SuiteResult, List[Journey])] =
    suiteList.toList.map(s => (s, s.journeys))

  def totalDuration: Duration =
    Duration.ofMillis(suiteList.map(s => s.duration.getOrElse(0L)).sum)

  def started: Instant =
    Instant.ofEpochMilli(suiteList.map(_.startEvent.timeStamp).min)
}
