package org.mlm.browkorftv.webengine.webview

object Scripts {
    const val LONG_PRESS_SCRIPT = """
var element = window.TVBRO_activeElement;
if (element != null) {
  if ('A' == element.tagName) {
    element.protocol+'//'+element.host+element.pathname+element.search+element.hash;
  } else if (element.src != null) {
    element.src;
  }
}"""
}