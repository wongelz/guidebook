package com.github.wongelz.guidebook

import org.openqa.selenium.Dimension
import org.scalatest.ConfigMap

import scala.util.matching.Regex

case class Screen(width: Int, height: Int) {
  def dimension: Dimension =
    new Dimension(width, height)
}

case class Screens(default: Screen, additionalScreenshots: List[Screen]) {
  def toList: List[Screen] =
    default :: additionalScreenshots
}

object Screens {

  val ScreenRegex: Regex = """(\d+)x(\d+)""".r
  val Default: Screen = Screen(1024, 768)

  def fromConfig(configMap: ConfigMap): Screens = {
    val defaultConfig = configMap.getOptional[String]("screen.default.size")
    val defaultScreen: Screen = defaultConfig.flatMap(getScreen).getOrElse(Default)

    val additional = (1 to 5).toList flatMap { i =>
      configMap.getOptional[String](s"screen.$i.size").flatMap(getScreen)
    }

    Screens(defaultScreen, additional)
  }


  def getScreen(str: String): Option[Screen] = {
    ScreenRegex.findFirstMatchIn(str).map { m =>
      Screen(m.group(1).toInt, m.group(2).toInt)
    }
  }
}
