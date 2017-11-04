package com.github.wongelz.guidebook

case class GuidebookNav(browsers: List[String], screens: Screens) {

  def location(browser: String, screen: Screen): String = {
    if (browsers.indexOf(browser) == 0 && screen == screens.default) {
      "index.html"
    } else {
      s"index_${browser.toLowerCase}-${screen.width}x${screen.height}.html"
    }
  }
}
