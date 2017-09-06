package com.github.wongelz.guidebook

import java.time.{Duration, Instant}

import scala.collection.mutable.ListBuffer

class SuiteResultHolder {

  val suiteList = new ListBuffer[SuiteResult]()

  def +=(result: SuiteResult): Unit = {
    suiteList += result
  }

  def journeys: List[Journey] =
    suiteList.toList.flatMap(_.journeys)

  def totalDuration: Duration =
    Duration.ofMillis(suiteList.map(s => s.duration.getOrElse(0L)).sum)

  def started: Instant =
    suiteList.map(_.timestamp).min
}
