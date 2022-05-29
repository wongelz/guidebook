package com.github.wongelz.guidebook.client

import com.github.wongelz.guidebook.client.bootstrap.Modal
import org.scalajs.dom
import org.scalajs.dom.{Element, Event, document}

import scala.scalajs.js
import scala.scalajs.js.JSON

case class Guidebook(steps: List[Step]) {

  val modal = document.querySelector("#modal")
  new Modal(modal)

  val alertContent = modal.querySelector("#modal-body-alerts")
  val screenshotContent = modal.querySelector("#modal-body-screenshot")
  val stacktraceContent = modal.querySelector("#modal-body-stacktrace")
  val toggleLink = modal.querySelector(".guidebook-modal-toggle")
  val pagination = modal.querySelector(".pagination")
  val caption = modal.querySelector(".modal-body figcaption")
  val badge = modal.querySelector(".modal-body .badge")
  val title = modal.querySelector(".modal-title")

  modal.addEventListener[dom.MouseEvent]("show.bs.modal", e => {
    e.relatedTarget match {
      case el: Element =>
        nonEmptyString(el.getAttribute("data-guidebook-step")) match {
          case Some(stepJson) =>
            val info = js.JSON.parse(stepJson)
            steps.find(_.id == info.id.toString) foreach display
          case None =>
            val target = el.getAttribute("guidebook-target")
            steps.find(_.id == target) foreach display
        }
      case t => println(s"Unknown target $t")
    }
  })

  toggleLink.addEventListener[Event]("click", _ => {
    if (toggleLink.textContent.contains("stacktrace")) {
      hide(screenshotContent)
      show(stacktraceContent)
      toggleLink.textContent = "[Show screenshot]"
    } else {
      hide(stacktraceContent)
      show(screenshotContent)
      toggleLink.textContent = "[Show stacktrace]"
    }
  })

  private def display(step: Step): Unit = {
    val alertDivs = step.alerts map { a =>
      val d = dom.document.createElement("div")
      d.setAttribute("class", "alert alert-danger")
      d.setAttribute("role", "alert")
      d.innerText = a
      d
    }
    val noteDivs = step.notes map { n =>
      val d = dom.document.createElement("div")
      d.setAttribute("class", "alert alert-info")
      d.setAttribute("role", "alert")
      d.innerText = n
      d
    }
    alertContent.replaceChildren(alertDivs ++ noteDivs:_*)

    screenshotContent.querySelector("a").setAttribute("href", step.screenshot)
    screenshotContent.querySelector("img").setAttribute("src", step.screenshot)
    title.innerHTML = step.journey
    caption.textContent = step.caption

    badge.innerHTML = step.result
    if (step.result == "Passed") {
      badge.setAttribute("class", "badge badge-success")
    } else {
      badge.setAttribute("class", "badge badge-danger")
    }

    step.stacktrace match {
      case Some(stacktrace) =>
        stacktraceContent.querySelector("code").innerText = stacktrace
        toggleLink.textContent = "[Show stacktrace]"
        show(toggleLink)
      case None =>
        hide(toggleLink)
    }

    show(screenshotContent)
    hide(stacktraceContent)
    updatePagination(step)
  }

  private def updatePagination(step: Step): Unit = {
    val stepsInJourney = steps.filter(_.journey == step.journey)
    val stepIds = stepsInJourney.map(_.id)
    val activeIdx = stepIds.indexOf(step.id)
    val prev = paginationItem(stepsInJourney, activeIdx - 1, "Prev", active = false)
    val nums = stepsInJourney.zipWithIndex map {
      case (s, i) =>
        paginationItem(stepsInJourney, i, (i + 1).toString, i == activeIdx)
    }
    val next = paginationItem(stepsInJourney, activeIdx + 1, "Next", active = false)

    pagination.replaceChildren(prev :: nums ++ List(next):_*)
  }

  private def paginationItem(stepsInJourney: List[Step], idx: Int, text: String, active: Boolean): Element = {
    if (idx < 0 || idx >= stepsInJourney.length) {
      val a = dom.document.createElement("a")
      a.setAttribute("class", "page-link")
      a.innerHTML = text

      val li = dom.document.createElement("li")
      li.setAttribute("class", "page-item disabled")
      li.appendChild(a)
      li
    } else {
      val step = stepsInJourney(idx)
      val a = dom.document.createElement("a")
      a.setAttribute("class", "page-link")
      a.setAttribute("href", "#")
      a.setAttribute("title", step.caption)
      a.addEventListener[Event]("click", _ => {
        display(step)
      })
      a.innerHTML = text

      val li = dom.document.createElement("li")
      li.setAttribute("class", if (active) "page-item active" else "page-item")
      li.appendChild(a)
      li
    }
  }

  private def show(e: Element) =
    e.setAttribute("style", "display: block")

  private def hide(e: Element) =
    e.setAttribute("style", "display: none")

  private def nonEmptyString(str: String): Option[String] =
    Option(str).filterNot(_.isEmpty)
}

object ClientMain {
  def main(args: Array[String]): Unit = {
    val steps = for {
      s <- document.querySelectorAll(".guidebook-step")
    } yield Step(s)

    Guidebook(steps.toList)
  }
}

case class Step(
  id: String,
  caption: String,
  result: String,
  journey: String,
  screenshot: String,
  stacktrace: Option[String],
  alerts: List[String],
  notes: List[String]
)

object Step {
  def apply(el: Element): Step = {
    val infoAttr = el.getAttribute("data-guidebook-step")
    val info = JSON.parse(infoAttr)
    Step(
      info.id.toString,
      info.caption.toString,
      info.result.toString,
      el.getAttribute("data-journey"),
      el.getAttribute("href"),
      Option(info.stacktrace).map(_.toString),
      info.alerts.asInstanceOf[js.Array[String]].toList,
      info.notes.asInstanceOf[js.Array[String]].toList
    )
  }
}
