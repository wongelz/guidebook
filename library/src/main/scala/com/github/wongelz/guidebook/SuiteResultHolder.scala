package com.github.wongelz.guidebook

import java.time.{Duration, Instant}

import scala.collection.mutable.ListBuffer

class SuiteResultHolder {

  val suiteList = new ListBuffer[SuiteResult]()

  def +=(result: SuiteResult): Unit = {
    suiteList += result
  }

  def browsers: List[String] =
    suiteList.flatMap(_.browsers).toList.distinct.sorted

  def browserResults(browserName: String): List[BrowserSuiteResult] =
    suiteList.toList.flatMap(_.getJourneys(browserName))

  def totalDuration: Duration =
    Duration.ofMillis(suiteList.map(s => s.duration.getOrElse(0L)).sum)

  def started: Instant =
    suiteList.map(_.timestamp).min
}
