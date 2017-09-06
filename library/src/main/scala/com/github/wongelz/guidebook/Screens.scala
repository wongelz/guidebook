package com.github.wongelz.guidebook

import org.openqa.selenium.Dimension

case class Screen(width: Int, height: Int, description: String, suffix: String, icon: String) {
  def dimension: Dimension =
    new Dimension(width, height)
}

object Screens {

  val Default = Screen(1024, 768, "Desktop (default)", "", "fa-desktop")
  val Mobile = Screen(640, 768, "Mobile", "-m", "fa-mobile")

  val All: List[Screen] = Default :: Mobile :: Nil
}
