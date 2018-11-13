package io.burt.akts

/**
 * Context under which each test is executed
 */
class TestContext internal constructor(internal val context: SpecificationDsl) {
  /**
   * Ensure the collaborator is evaluated
   */
  fun <T> force(collaborator: TestContext.() -> T) {
    this.collaborator()
  }
  /**
   * Ensure the collaborator is evalated
   */
  fun <T> force(@Suppress("UNUSED_PARAMETER") collaborator: T) { }

  @Deprecated(message = "describe inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun describe(what: String, spec: SpecificationDsl.() -> Unit) { }

  @Deprecated(message = "context inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun context(case: String, spec: SpecificationDsl.() -> Unit) { }

  @Deprecated(message = "it inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun it(description: String, body: TestContext.() -> Unit) { }

  @Deprecated(message = "support inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun <T> support(destructor: (TestContext.(T) -> Unit)? = null, constructor: TestContext.() -> T) { }

  @Deprecated(message = "subject inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun <T> subject(destructor: (TestContext.(T) -> Unit)? = null, constructor: TestContext.() -> T) { }

  @Deprecated(message = "refine inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun <T> refine(collaborator: TestContext.() -> T, refinement: TestContext.(T) -> T) { }
}
