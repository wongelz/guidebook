package com.github.wongelz.guidebook

case class GuidebookNav(browsers: List[String], screens: List[Screen]) {

  def location(browser: String, screen: Screen): String = {
    if (browsers.indexOf(browser) == 0 && screens.indexOf(screen) == 0) {
      "index.html"
    } else {
      s"index_${browser.toLowerCase}${screen.suffix}.html"
    }
  }
}
