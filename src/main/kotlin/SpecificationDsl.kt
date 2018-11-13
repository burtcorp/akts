package io.burt.akts

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import java.net.URI
import kotlin.streams.asStream

/**
 * Container type for the specification DSL
 */
abstract class SpecificationDsl internal constructor(
  private val builder: MutableList<DynamicNode>,
  private val contextDescription: String
) {
  internal abstract val root: Root
  internal abstract val ancestors: List<SpecificationDsl>
  private val supports = mutableListOf<Collaborator<*>>()

  /**
   * Define a subspecification for a particular thing
   * @param what the described thing (e.g. "#methodName")
   * @param spec the specification body
   */
  fun describe(what: String, spec: SpecificationDsl.() -> Unit) {
    builder += DynamicContainer.dynamicContainer(
      what,
      mutableListOf<DynamicNode>().also {
        Child(it, what, this).spec()
      }.asSequence().asStream()
    )
  }

  /**
   * Define a subspecification for a particular situation
   * @param case the described situation (e.g. "when X is empty")
   * @param spec the specification body
   */
  fun context(case: String, spec: SpecificationDsl.() -> Unit) = describe(case, spec)

  /**
   * Define a specification example
   */
  fun it(description: String, body: TestContext.() -> Unit) {
    builder += DynamicTest.dynamicTest(
      fullDescription(description),
      if (root.isFlatNamespace) URI("flat") else null) {
      TestContext(this).also { instance ->
        instance.body()
        for (support in supports) {
          support.cleanup(instance)
        }
      }
    }
  }

  private fun fullDescription(exampleDescription: String) =
    if (root.isFlatNamespace) {
      ancestors.joinToString(separator = " ", postfix = " $exampleDescription") { it.contextDescription }
    }
    else {
      exampleDescription
    }

  /**
   * Define a [refine]able supporting collaborator, which will be instantiated exactly once for each test instance.
   * @param destructor is called once after test finishes with the refined value
   */
  fun <T> support(destructor: (TestContext.(T) -> Unit)? = null, constructor: TestContext.() -> T): TestContext.() -> T =
    Collaborator<T>(constructor, destructor).also { supports.add(it) }

  /**
   * Define a [refine]able subject under test, which will be instantiated exactly once for each test instance.
   * @param destructor is called once after test finishes with the refined value
   */
  fun <T> subject(destructor: (TestContext.(T) -> Unit)? = null, constructor: TestContext.() -> T): TestContext.() -> T =
    support(destructor, constructor)

  /**
   * Refine a collaborator by replacing the original definition with the result of the refinement
   */
  fun <T> refine(collaborator: TestContext.() -> T, refinement: TestContext.(T) -> T) {
    if (collaborator is Collaborator<T>) {
      collaborator.refine(this, refinement)
    } else {
      throw IllegalArgumentException("Only support and subject collaborators can be refined, not $collaborator")
    }
  }

  internal class Root(
    builder: MutableList<DynamicNode>,
    contextDescription: String,
    internal val isFlatNamespace: Boolean = false
  ): SpecificationDsl(builder, contextDescription) {
    override val root = this
    override val ancestors = listOf(this)
  }

  private class Child(
    builder: MutableList<DynamicNode>,
    contextDescription: String,
    private val parent: SpecificationDsl
  ): SpecificationDsl(builder, contextDescription) {
    override val root get() = parent.root
    override val ancestors get() = parent.ancestors + this
  }
}
