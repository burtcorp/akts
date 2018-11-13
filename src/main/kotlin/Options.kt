package io.burt.akts

internal object Options {
  val isFlatNamespace by lazy { System.getProperty("io.burt.akts.flat.namespace") == "true" }
}
