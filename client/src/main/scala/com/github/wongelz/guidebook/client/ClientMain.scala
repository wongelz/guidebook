package com.github.wongelz.guidebook.client

import com.github.wongelz.guidebook.client.Popover.JQueryPopover._
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery => $}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

object ClientMain {
  def main(args: Array[String]): Unit = {
    val steps = new Steps($(".guidebook-step"))
    new GuidebookModal($("#modal"), steps)

    $("[data-toggle=\"popover\"]").popover()
  }
}

@js.native
trait Step extends js.Object {
  val id: String = js.native
  val caption: String = js.native
  val result: String = js.native
  val stacktrace: UndefOr[String] = js.native
  val alerts: js.Array[String] = js.native
  val notes: js.Array[String] = js.native
  val journey: String = js.native
  val screenshot: String = js.native
}

class Steps(jQuery: JQuery) {

  val steps: js.Array[Step] =
    jQuery.toArray().map(e => toStep($(e)))

  private def toStep(element: JQuery): Step = {
    val step = element.data("guidebook-step")
    step.updateDynamic("journey")(element.data("journey"))
    step.updateDynamic("screenshot")(element.attr("href"))
    step.asInstanceOf[Step]
  }

  def findByJourney(journey: String): js.Array[Step] =
    steps.filter(_.journey == journey)

  def getById(id: String): Step =
    steps.filter(_.id == id).head
}

class GuidebookModal(modal: JQuery, steps: Steps) {

  private val screenshotContent = modal.find("#modal-body-screenshot")
  private val stacktraceContent = modal.find("#modal-body-stacktrace")
  private val toggleLink = modal.find(".guidebook-modal-toggle")
  private val pagination = new Pagination(modal.find(".pagination"), steps)
  private val caption = modal.find(".modal-body figcaption")
  private val badge = modal.find(".modal-body .badge")

  modal.on("show.bs.modal", (event: JQueryEventObject, element: js.Any) => {
    val target = $(event.relatedTarget)

    val step = if (target.attr("data-guidebook-step").isDefined) {
      steps.getById(target.data("guidebook-step").selectDynamic("id").asInstanceOf[String])
    } else {
      steps.getById(target.data("guidebook-target").asInstanceOf[String])
    }

    if (target.hasClass("guidebook-stacktrace")) {
      showStacktrace(step)
    } else {
      showScreenshot(step)
    }
  })

  modal.find(".pagination").on("paginate", (event: JQueryEventObject, id: String) => {
    showScreenshot(steps.getById(id))
  })

  toggleLink.click((event: JQueryEventObject) => {
    event.preventDefault()
    val step = getCurrentStep()
    if (screenshotContent.is(":visible")) {
      showStacktrace(step)
      toggleLink.text("[Show screenshot]")
    } else {
      showScreenshot(step)
      toggleLink.text("[Show stacktrace]")
    }
  })

  private def showAlerts(step: Step) = {
    def popover(icon: String, textColor: String, title: String, content: String): JQuery = {
      $("<button/>")
        .addClass("btn-link")
        .addClass("guidebook-info")
        .addClass(textColor)
        .attr("title", title)
        .attr("data-toggle", "popover")
        .attr("data-trigger", "hover")
        .attr("data-placement", "top")
        .attr("data-content", content)
        .append($("<i/>")
          .addClass("fa")
          .addClass(icon))
        .popover()
    }

    step.alerts foreach { a =>
      caption.append(popover("fa-exclamation-triangle", "text-warning", "Alert", a))
    }
    step.notes foreach { n =>
      caption.append(popover("fa-info-circle", "text-info", "Note", n))
    }
  }

  private def showStep(step: Step) = {
    modal.data("current-step", step.id)
    modal.find(".modal-title").html(step.journey)
    caption.text(step.caption)
    badge.html(step.result)
    if (step.result == "Passed") {
      badge.removeClass("badge-danger").addClass("badge-success")
    } else {
      badge.removeClass("badge-success").addClass("badge-danger")
    }
    showAlerts(step)
    pagination.update(step)
  }

  private def showScreenshot(step: Step) = {
    showStep(step)
    screenshotContent.find("a").attr("href", step.screenshot)
    screenshotContent.find("img").attr("src", step.screenshot)

    if (step.result == "Passed") {
      toggleLink.hide()
    } else {
      toggleLink.text("[Show stacktrace]").show()
    }

    stacktraceContent.hide()
    screenshotContent.show()
  }

  private def showStacktrace(step: Step) = {
    showStep(step)
    stacktraceContent.find("code").empty()
    stacktraceContent.find("code").text(step.stacktrace.getOrElse(""))

    toggleLink.text("[Show screenshot]").show()

    screenshotContent.hide()
    stacktraceContent.show()
  }

  private def getCurrentStep(): Step = {
    val id = modal.data("current-step").asInstanceOf[String]
    steps.getById(id)
  }
}

class Pagination(jq: JQuery, steps: Steps) {
  def update(currentStep: Step): Unit = {
    val stepsInJourney = steps.findByJourney(currentStep.journey)
    val stepIds = stepsInJourney.map(_.id)
    val activeIdx = stepIds.indexOf(currentStep.id)

    jq.empty()
    jq.append(item(stepsInJourney, activeIdx - 1, "Prev", active = false))
    stepsInJourney.zipWithIndex foreach {
      case (s, i) =>
        jq.append(item(stepsInJourney, i, (i + 1).toString, i == activeIdx))
    }
    jq.append(item(stepsInJourney, activeIdx + 1, "Next", active = false))
  }

  private def item(stepsInJourney: js.Array[Step], idx: Int, text: String, active: Boolean) = {
    if (idx < 0 || idx >= stepsInJourney.length) {
      $("<li/>")
        .addClass("page-item")
        .addClass("disabled")
        .append($("<a/>")
          .addClass("page-link")
          .html(text))
    } else {
      val step = stepsInJourney(idx)
      val li = $("<li/>")
        .addClass("page-item")
        .append($("<a/>")
          .addClass("page-link")
          .html(text)
          .attr("href", step.screenshot)
          .attr("title", step.caption)
          .click((event: JQueryEventObject) => {
            event.preventDefault()
            jq.trigger("paginate", step.id)
          }))
      if (active) {
        li.addClass("active")
      } else {
        li
      }
    }
  }
}
