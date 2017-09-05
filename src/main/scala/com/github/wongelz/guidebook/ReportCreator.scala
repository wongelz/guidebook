package com.github.wongelz.guidebook

import java.io.{ByteArrayOutputStream, PrintStream}
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import scalatags.Text.all._
import scalatags.Text.tags2

object ReportCreator {

  private val dateFormat = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy")

  def create(results: SuiteResultHolder, screen: Screen): Frag = {
    val journeys = results.journeys
    val passed = journeys.forall(_.passed)

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
        div(cls := "container-fluid")(
          ul(cls := "nav justify-content-end")(
            Screens.All.map { s =>
              li(cls := "nav-item")(
                a(cls := "nav-link btn btn-default", href := s"index${s.suffix}.html", title := s.description)(
                  i(cls := s"fa fa-sm ${s.icon}")
                )
              )
            }
          ),
          div(cls := (if (passed) "card mb-3 card-inverse card-success" else "card mb-3 card-inverse card-danger"))(
            div(cls := "card-block")(
              h4(cls := "card-title")(if (passed) "Summary - Passed" else "Summary - FAILED!!!"),
              blockquote(cls := "card-blockquote")(
                p(s"Time: ${dateFormat.format(results.started.atZone(ZoneId.systemDefault()))}"),
                p(s"Duration: ${results.totalDuration.toMinutes} minutes ${results.totalDuration.getSeconds % 60} seconds"),
                p(s"Journeys completed: ${journeys.count(_.passed)} of ${journeys.length}"),
                p(s"Steps completed: ${journeys.foldLeft(0)(_ + _.passedStepCount)} of ${journeys.foldLeft(0)(_ + _.stepCount)}")
              )
            )
          ),
          results.suiteList.map { r =>
            Seq[Frag](
              h3(r.suiteName),
              getJourneysHtml(r.journeys, Nil, screen)
            )
          },
          modal
        ),
        script(src := "https://code.jquery.com/jquery-3.1.1.slim.min.js"),
        script(src := "https://cdnjs.cloudflare.com/ajax/libs/tether/1.4.0/js/tether.min.js"),
        script(src := "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.6/js/bootstrap.min.js"),
        script(src := "app.js")
      )
    )
  }

  private def modal =
    div(id := "modal", cls := "modal fade", tabindex := "-1", role := "dialog")(
      div(cls := "modal-dialog modal-lg", role := "document")(
        div(cls := "modal-content")(
          div(cls := "modal-header")(
            h5(cls := "modal-title")("Failure"),
            button(`type` := "button", cls := "close", data("dismiss") := "modal", aria.label := "Close")(
              span(aria.hidden := "true")(raw("&times;"))
            )
          ),
          div(id := "modal-body-screenshot", cls := "modal-body hidden")(
            figure(cls := "thumbnail")(
              a(target := "_blank")(
                img(cls := "img-fluid img-thumbnail")
              ),
              figcaption(cls := "figure-caption")
            )
          ),
          div(id := "modal-body-cause", cls := "modal-body hidden")(
            pre(
              code(
              )
            ),
          ),
          div(id := "modal-body-message", cls := "modal-body hidden")(

          ),
          div(cls := "modal-footer hidden")(
            tags2.nav(
              ul(cls := "pagination pagination-sm justify-content-center")
            )
          )
        )
      )
    )

  private def getJourneysHtml(journeys: List[Journey], scope: List[String], screen: Screen): List[Frag] = {
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
        {heading} :: {getJourneyHtml(j, screen)} :: getJourneysHtml(js, j.scope, screen)
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

  private def getJourneyHtml(journey: Journey, screen: Screen) =
    div(cls := "row row-eq-height")(
      journey.steps.map { s =>
        div(cls := "col-lg-2 col-md-3 col-sm-4")(
          figure(cls := "thumbnail")(
            s.result match {
              case Result.Passed =>
                a(cls := "screenshot", href := s.screenshot(screen), data("toggle") := "modal", data("target") := "#modal", data("step") := s.id.hash, data("journey") := journey.description, title := s.caption)(
                  img(cls := "figure-img img-fluid img-thumbnail bg-success", src := s.screenshot(screen))
                )
              case Result.Failed =>
                a(cls := "screenshot", href := s.screenshot(screen), data("toggle") := "modal", data("target") := "#modal", data("step") := s.id.hash, data("journey") := journey.description, title := s.caption)(
                  img(cls := "figure-img img-fluid img-thumbnail bg-danger", src := s.screenshot(screen))
                )
              case Result.Canceled =>
                img(cls := "figure-img img-fluid img-thumbnail bg-faded", src := s"http://via.placeholder.com/${screen.width}x${screen.height}/f7f7f7/636c72?text=Canceled")
              case Result.Ignored =>
                img(cls := "figure-img img-fluid img-thumbnail bg-warning", src := s"http://via.placeholder.com/${screen.width}x${screen.height}/f2dede/ffffff?text=Ignored")
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

  private def getStackTrace(th: Throwable): String = {
    val out = new ByteArrayOutputStream()
    th.printStackTrace(new PrintStream(out))
    new String(out.toByteArray)
  }
}
