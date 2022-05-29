package com.github.wongelz.guidebook.client.bootstrap

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("bootstrap.bundle.js", "Popover")
object Popover extends js.Object {

}

case class PopoverOptions(
                           animation: Boolean = true,
                           container: String = "",
                           delay: String = "",
                           html: Boolean = false,
                           selector: String = "",
                           template: String = "",
                           trigger: String = "",
                           viewport: String = ""
                         )