package com.github.wongelz.guidebook.client

import org.scalajs.jquery.JQuery

import scala.scalajs.js

object Popover {
  @js.native
  trait JQueryPopover extends JQuery {
    def popover(): JQueryPopover = js.native
  }

  object JQueryPopover {
    implicit def jqpopover(jq: JQuery): JQueryPopover =
      jq.asInstanceOf[JQueryPopover]
  }
}
