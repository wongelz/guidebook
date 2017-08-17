package com.github.wongelz.guidebook

import java.io._
import java.net.URL
import java.nio.channels.Channels
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
  copyResource(getResource("app.js"), targetDir, "app.js")

  private var eventList = new ListBuffer[Event]()
  private var runEndEvent: Option[Event] = None
  private val results = new SuiteResultHolder()

  val dateFormat = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy")

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

      case SuiteCompleted(ordinal, suiteName, suiteId, suiteClassName, duration, formatter, location, rerunner, payload, threadName, timeStamp) =>
        val (suiteEvents, otherEvents) = extractSuiteEvents(suiteId)
        eventList = otherEvents
        val sortedSuiteEvents = suiteEvents.sorted
        if (sortedSuiteEvents.isEmpty)
          throw new IllegalStateException("Expected SuiteStarting for completion event: " + event + " in the head of suite events, but we got no suite event at all")
        sortedSuiteEvents.head match {
          case suiteStarting: SuiteStarting =>
            val suiteResult = sortedSuiteEvents.foldLeft(SuiteResult(suiteId, suiteName, suiteClassName, duration, suiteStarting, event, Vector.empty ++ sortedSuiteEvents.tail, 0, 0, 0, 0, 0, 0, true)) { case (r, e) =>
              e match {
                case testSucceeded: TestSucceeded => r.copy(testsSucceededCount = r.testsSucceededCount + 1)
                case testFailed: TestFailed => r.copy(testsFailedCount = r.testsFailedCount + 1)
                case testIgnored: TestIgnored => r.copy(testsIgnoredCount = r.testsIgnoredCount + 1)
                case testPending: TestPending => r.copy(testsPendingCount = r.testsPendingCount + 1)
                case testCanceled: TestCanceled => r.copy(testsCanceledCount = r.testsCanceledCount + 1)
                case scopePending: ScopePending => r.copy(scopesPendingCount = r.scopesPendingCount + 1)
                case _ => r
              }
            }

            val suiteStartingEvent = sortedSuiteEvents.head.asInstanceOf[SuiteStarting]

            if (!suiteStartingEvent.formatter.contains(MotionToSuppress)) {
              results += suiteResult
              makeSuiteFile(suiteResult)
            }
          case other =>
            throw new IllegalStateException("Expected SuiteStarting for completion event: " + event +  " in the head of suite events, but we got: " + other)
        }

      case SuiteAborted(ordinal, message, suiteName, suiteId, suiteClassName, throwable, duration, formatter, location, rerunner, payload, threadName, timeStamp) =>
        val (suiteEvents, otherEvents) = extractSuiteEvents(suiteId)
        eventList = otherEvents
        val sortedSuiteEvents = suiteEvents.sorted
        if (sortedSuiteEvents.isEmpty)
          throw new IllegalStateException("Expected SuiteStarting for completion event: " + event + " in the head of suite events, but we got no suite event at all")
        sortedSuiteEvents.head match {
          case suiteStarting: SuiteStarting =>
            val suiteResult = sortedSuiteEvents.foldLeft(SuiteResult(suiteId, suiteName, suiteClassName, duration, suiteStarting, event, Vector.empty ++ sortedSuiteEvents.tail, 0, 0, 0, 0, 0, 0, false)) { case (r, e) =>
              e match {
                case testSucceeded: TestSucceeded => r.copy(testsSucceededCount = r.testsSucceededCount + 1)
                case testFailed: TestFailed => r.copy(testsFailedCount = r.testsFailedCount + 1)
                case testIgnored: TestIgnored => r.copy(testsIgnoredCount = r.testsIgnoredCount + 1)
                case testPending: TestPending => r.copy(testsPendingCount = r.testsPendingCount + 1)
                case testCanceled: TestCanceled => r.copy(testsCanceledCount = r.testsCanceledCount + 1)
                case scopePending: ScopePending => r.copy(scopesPendingCount = r.scopesPendingCount + 1)
                case _ => r
              }
            }
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
    makeIndexFile()
  }

  private def makeSuiteFile(suiteResult: SuiteResult): Unit = {
    // TODO
  }

  private def makeIndexFile(): Unit = {
    val pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
      new File(targetDir, "index.html")), 4096), "UTF-8"))
    try {
      pw.println {
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
          "<!DOCTYPE html\n" +
          "PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" +
          "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
          makeIndexHtml()
      }
    } finally {
      pw.flush()
      pw.close()
    }
  }

  private def makeIndexHtml() = {
    val suiteJourneys = results.suiteJourneys
    val journeys = suiteJourneys.flatMap(_._2)
    html(
      head(
        scalatags.Text.tags2.title("Guidebook"),
        meta(httpEquiv := "Content-Type", content := "text/html; charset=utf-8"),
        meta(httpEquiv := "Expires", content := "-1"),
        meta(httpEquiv := "Pragma", content := "no-cache"),
        link(rel := "stylesheet", href := "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.6/css/bootstrap.min.css"),
        link(rel := "stylesheet", href := "https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css"),
        link(rel := "stylesheet", href := "app.css")
      ),
      body(
        div(cls := "container")(
          div(cls := (if (journeys.forall(_.passed)) "card mb-3 card-inverse card-success" else "card mb-3 card-inverse card-danger"))(
            div(cls := "card-block")(
              h4(cls := "card-title")(if (journeys.forall(_.passed)) "Summary - Passed" else "Summary - FAILED!!!"),
              blockquote(cls := "card-blockquote")(
                p(s"Time: ${dateFormat.format(results.started.atZone(ZoneId.systemDefault()))}"),
                p(s"Duration: ${results.totalDuration.toMinutes} minutes ${results.totalDuration.getSeconds % 60} seconds"),
                p(s"Journeys completed: ${journeys.count(_.passed)} of ${journeys.length}"),
                p(s"Steps completed: ${journeys.foldLeft(0)(_ + _.passedStepCount)} of ${journeys.foldLeft(0)(_ + _.stepCount)}")
              )
            )
          ),
          suiteJourneys.map {
            case (s, js) =>
              Seq[Frag](
                h3(s.suiteName),
                getJourneysHtml(js, Nil)
              )
          },
          div(id := "modal", cls := "modal fade", tabindex := "-1", role := "dialog")(
            div(cls := "modal-dialog modal-lg", role := "document")(
              div(cls := "modal-content")(
                div(cls := "modal-header")(
                  h5(cls := "modal-title")("Failure"),
                  button(`type` := "button", cls := "close", data("dismiss") := "modal", aria.label := "Close")(
                    span(aria.hidden := "true")(raw("&times;"))
                  )
                ),
                div(id := "modal-body-cause", cls := "modal-body", cls := "hidden")(
                  pre(
                    code(
                    )
                  ),
                ),
                div(id := "modal-body-other", cls := "modal-body")
              )
            )
          )
        ),
        script(src := "https://code.jquery.com/jquery-3.1.1.slim.min.js"),
        script(src := "https://cdnjs.cloudflare.com/ajax/libs/tether/1.4.0/js/tether.min.js"),
        script(src := "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.6/js/bootstrap.min.js"),
        script(src := "app.js")
      )
    )
  }

  private def getJourneysHtml(journeys: List[Journey], scope: List[String]): List[Frag] = {
    journeys match {
      case Nil => Nil
      case j :: js =>
        val heading = (j.scope, scope) match {
          case (s :: s1, _ :: s2) if s1 == s2 =>
            div(cls := "row")(
              h5(cls := "col-sm-12")(
                s,
                getResultBadge(j)
              )
            )
          case (s :: s1, _) =>
            div(cls := "row")(
              h4(cls := "col-sm-12")(s1.reverse.mkString(" ")),
              h5(cls := "col-sm-12")(
                s,
                getResultBadge(j)
              )
            )
        }
        {heading} :: {getThumbnailsHtml(j.steps.reverse)} :: getJourneysHtml(js, j.scope)
    }
  }

  private def getResultBadge(journey: Journey) = {
    val text = s"Completed ${journey.passedStepCount} of ${journey.stepCount} (${journey.percentComplete}%)"
    if (journey.passed) {
      span(cls := "badge badge-success float-right")(text)
    } else {
      span(cls := "badge badge-danger float-right")(text)
    }
  }

  private def getThumbnailsHtml(steps: List[Step]) =
    div(cls := "row row-eq-height")(
      steps.map { s =>
        div(cls := "col-lg-2 col-md-3 col-sm-4")(
          figure(cls := "thumbnail")(
            s.result match {
              case Result.Passed =>
                a(cls := "screenshot", href := s.screenshot, data("toggle") := "modal", data("target") := "#modal", title := s.caption)(
                  img(cls := "figure-img img-fluid img-thumbnail bg-success", src := s.screenshot)
                )
              case Result.Failed =>
                a(cls := "screenshot", href := s.screenshot, data("toggle") := "modal", data("target") := "#modal", title := s.caption)(
                  img(cls := "figure-img img-fluid img-thumbnail bg-danger", src := s.screenshot)
                )
              case Result.Canceled =>
                a(href := "#", title := s.caption)(
                  img(cls := "figure-img img-fluid img-thumbnail bg-warning", src := "http://via.placeholder.com/139x118/f2dede/ffffff?text=Canceled")
                )
              case Result.Ignored =>
                a(href := "#", title := s.caption)(
                  img(cls := "figure-img img-fluid img-thumbnail bg-warning", src := "http://via.placeholder.com/139x118/f2dede/ffffff?text=Ignored")
                )
            },
            figcaption(cls := "figure-caption")(
              s.caption,
              getStackTraceLink(s) ++ getAlertLinks(s) ++ getNoteLinks(s)
            )
          )
        )
      }
    )

  private def getAlertLinks(s: Step): List[Frag] = {
    s.alerts.map { note =>
      a(cls := "info text-warning", href := "#", title := s.caption + " - alert", data("toggle") := "modal", data("target") := "#modal", data("info") := note)(
        i(cls := "fa fa-exclamation-triangle", aria.hidden := "true")
      )
    }
  }

  private def getNoteLinks(s: Step): List[Frag] = {
    s.notes.map { note =>
      a(cls := "info text-info", href := "#", title := s.caption + " - note", data("toggle") := "modal", data("target") := "#modal", data("info") :=  note)(
        i(cls := "fa fa-info-circle", aria.hidden := "true")
      )
    }
  }

  private def getStackTraceLink(s: Step): List[Frag] = {
    s.throwable match {
      case Some(th) if s.result == Result.Failed =>
        List(
          a(cls := "cause text-danger", href := "#", alt := s.caption + " - failed", data("toggle") := "modal", data("target") := "#modal", data("cause") := getStackTrace(th))(
            i(cls := "fa fa-question-circle", aria.hidden := "true")
          )
        )
      case _ => Nil
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

  def getStackTrace(th: Throwable): String = {
    val out = new ByteArrayOutputStream()
    th.printStackTrace(new PrintStream(out))
    new String(out.toByteArray)
  }
}