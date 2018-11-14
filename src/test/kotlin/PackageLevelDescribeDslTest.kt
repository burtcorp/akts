package io.burt.akts

class PackageLevelDescribeDslTest: AbstractDescribeDslTest() {
  override fun <T> describeWithOptions(type: Class<T>, isFlatNamespace: Boolean, spec: SpecificationDsl.() -> Unit) =
    io.burt.akts.describe(type, isFlatNamespace, spec)

  override fun <T> describeWithDefaultOptions(type: Class<T>, spec: SpecificationDsl.() -> Unit) =
    io.burt.akts.describe(type, spec)
}
