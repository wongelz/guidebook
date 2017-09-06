package com.github.wongelz.guidebook

import java.io._
import java.net.URL
import java.nio.channels.Channels

import org.scalatest.ResourcefulReporter
import org.scalatest.events._

import scala.collection.mutable.ListBuffer
import scalatags.Text.all._

class GuidebookReporter extends ResourcefulReporter {
  import GuidebookReporter._

  private val targetDir = new File("target", "guidebook")

  if (!targetDir.exists)
    targetDir.mkdirs()

  copyResource(getResource("app.css"), targetDir, "app.css")
  copyResource(getResource("assets/client-jsdeps.min.js"), targetDir, "client-jsdeps.min.js")
  copyResource(getResource("assets/client-opt.js"), targetDir, "client-opt.js")
  copyResource(getResource("assets/client-opt.js.map"), targetDir, "client-opt.js.map")

  private var eventList = new ListBuffer[Event]()
  private var runEndEvent: Option[Event] = None
  private val results = new SuiteResultHolder()

  def apply(event: Event): Unit = {
    event match {
      case _: DiscoveryStarting  =>
      case _: DiscoveryCompleted =>

      case RunStarting(ordinal, testCount, configMap, formatter, location, payload, threadName, timeStamp) =>

      case RunCompleted(ordinal, duration, summary, formatter, location, payload, threadName, timeStamp) =>
        runEndEvent = Some(event)

      case RunStopped(ordinal, duration, summary, formatter, location, payload, threadName, timeStamp) =>
        runEndEvent = Some(event)

      case RunAborted(ordinal, message, throwable, duration, summary, formatter, location, payload, threadName, timeStamp) =>
        runEndEvent = Some(event)

      case suiteCompleted @ SuiteCompleted(ordinal, suiteName, suiteId, suiteClassName, duration, formatter, location, rerunner, payload, threadName, timeStamp) =>
        val (suiteEvents, otherEvents) = extractSuiteEvents(suiteId)
        eventList = otherEvents
        val sortedSuiteEvents = suiteEvents.sorted.toList
        if (sortedSuiteEvents.isEmpty)
          throw new IllegalStateException("Expected SuiteStarting for completion event: " + event + " in the head of suite events, but we got no suite event at all")
        sortedSuiteEvents.head match {
          case suiteStarting: SuiteStarting =>
            val suiteResult = SuiteResult(suiteStarting, suiteCompleted, sortedSuiteEvents.tail)

            if (!suiteStarting.formatter.contains(MotionToSuppress)) {
              results += suiteResult
              makeSuiteFile(suiteResult)
            }
          case other =>
            throw new IllegalStateException("Expected SuiteStarting for completion event: " + event +  " in the head of suite events, but we got: " + other)
        }

      case suiteAborted @ SuiteAborted(ordinal, message, suiteName, suiteId, suiteClassName, throwable, duration, formatter, location, rerunner, payload, threadName, timeStamp) =>
        val (suiteEvents, otherEvents) = extractSuiteEvents(suiteId)
        eventList = otherEvents
        val sortedSuiteEvents = suiteEvents.sorted.toList
        if (sortedSuiteEvents.isEmpty)
          throw new IllegalStateException("Expected SuiteStarting for completion event: " + event + " in the head of suite events, but we got no suite event at all")
        sortedSuiteEvents.head match {
          case suiteStarting: SuiteStarting =>
            val suiteResult = SuiteResult(suiteStarting, suiteAborted, sortedSuiteEvents.tail)
            results += suiteResult
            makeSuiteFile(suiteResult)
          case other =>
            throw new IllegalStateException("Expected SuiteStarting for completion event: " + event + " in the head of suite events, but we got: " + other)
        }

      case _ => eventList += event
    }
  }

  private def extractSuiteEvents(suiteId: String) = eventList partition {
    case e: TestStarting => e.suiteId == suiteId
    case e: TestSucceeded => e.suiteId == suiteId
    case e: TestIgnored => e.suiteId == suiteId
    case e: TestFailed => e.suiteId == suiteId
    case e: TestPending => e.suiteId == suiteId
    case e: TestCanceled => e.suiteId == suiteId
    case e: InfoProvided =>
      e.nameInfo match {
        case Some(nameInfo) =>
          nameInfo.suiteId == suiteId
        case None => false
      }
    case e: AlertProvided =>
      e.nameInfo match {
        case Some(nameInfo) =>
          nameInfo.suiteId == suiteId
        case None => false
      }
    case e: NoteProvided =>
      e.nameInfo match {
        case Some(nameInfo) =>
          nameInfo.suiteId == suiteId
        case None => false
      }
    case e: MarkupProvided =>
      e.nameInfo match {
        case Some(nameInfo) =>
          nameInfo.suiteId == suiteId
        case None => false
      }
    case e: ScopeOpened => e.nameInfo.suiteId == suiteId
    case e: ScopeClosed => e.nameInfo.suiteId == suiteId
    case e: ScopePending => e.nameInfo.suiteId == suiteId
    case e: SuiteStarting => e.suiteId == suiteId
    case _ => false
  }

  def dispose(): Unit = {
    for (s <- Screens.All) {
      makeIndexFile(ReportCreator.create(results, s), s)
    }
  }

  private def makeSuiteFile(suiteResult: SuiteResult): Unit = {
    // TODO
  }

  private def makeIndexFile(contents: Frag, screen: Screen): Unit = {
    val pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
      new File(targetDir, s"index${screen.suffix}.html")), 4096), "UTF-8"))
    try {
      pw.println {
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<!DOCTYPE html\n" +
          "PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" +
          "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
          contents
      }
    } finally {
      pw.flush()
      pw.close()
    }
  }
}

object GuidebookReporter {
  def copyResource(url: URL, toDir: File, targetFileName: String): Unit = {
    val inputStream = url.openStream
    try {
      val outputStream = new FileOutputStream(new File(toDir, targetFileName))
      try {
        outputStream getChannel() transferFrom(Channels.newChannel(inputStream), 0, Long.MaxValue)
      }
      finally {
        outputStream.flush()
        outputStream.close()
      }
    }
    finally {
      inputStream.close()
    }
  }

  def getResource(resourceName: String): URL =
    classOf[GuidebookReporter].getClassLoader.getResource(resourceName)

}