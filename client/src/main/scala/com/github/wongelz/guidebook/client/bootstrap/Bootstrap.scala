package com.github.wongelz.guidebook.client.bootstrap

import org.scalajs.dom
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

@js.native
@JSImport("bootstrap/dist/js/bootstrap.bundle.js", JSImport.Default)
object Bootstrap extends js.Object

@js.native
@JSImport("bootstrap/dist/js/bootstrap.bundle.js", "Modal")
class Modal(el: Element) extends js.Object {

}
//
//@js.native
//trait PopoverOptions extends js.Object {
//  var animation: Boolean         = js.native
//  var container: js.Any          = js.native
//  var content: String | dom.Node = js.native
//  var delay: js.Any              = js.native
//  var html: Boolean              = js.native
//  var placement: js.Any          = js.native
//  var selector: js.Any           = js.native
//  var template: String           = js.native
//  var title: String              = js.native
//  var trigger: String            = js.native
//  var viewport: js.Any           = js.native
//}
//
//@js.native
//@JSImport("bootstrap/dist/js/bootstrap.bundle.js", "Popover")
//class Popover(val options: PopoverOptions)
//
//object Popover {
//  def apply(title: String, content: String) =
//    Popover(PopoverOptions(title = title, content = content))
//}