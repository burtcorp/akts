package io.burt.akts

import kotlin.streams.asStream

class AktsScriptDescribeDslTest: AbstractDescribeDslTest() {
  override fun <T> describeWithOptions(type: Class<T>, isFlatNamespace: Boolean, spec: SpecificationDsl.() -> Unit) =
    object: AktsScript() { init { this.describe(type, isFlatNamespace, spec) } }.tests().asSequence().asStream()

  override fun <T> describeWithDefaultOptions(type: Class<T>, spec: SpecificationDsl.() -> Unit) =
    object: AktsScript() { init { this.describe(type, spec) } }.tests().asSequence().asStream()
}

