package io.burt.akts

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream
import kotlin.coroutines.experimental.buildSequence
import kotlin.streams.asStream

/**
 * Create a new test factory describing the specified class
 *
 * @param T the described class type
 * @param spec the specification body
 */
inline fun <reified T> describe(noinline spec: Specification.() -> Unit) = describe(T::class.java, spec)

/**
 * Create a new test factory describing the specified class
 *
 * @param T the described class type
 * @param type the described class instance
 * @param spec the specification body
 */
fun <T> describe(type: Class<T>, spec: Specification.() -> Unit) : Stream<DynamicNode> =
  sequenceOf(DynamicContainer.dynamicContainer(
    type.simpleName,
    mutableListOf<DynamicNode>().also {
      Specification(it, null).spec()
    }.toList()
  )).asStream()


class Specification internal constructor(private val builder: MutableList<DynamicNode>, parent: Specification?) {
  internal val ancestors: List<Specification> = (parent?.ancestors ?: emptyList<Specification>()) + listOf(this)
  private val supports = mutableListOf<Collaborator<*>>()

  /**
   * Define a subspecification for a particular thing
   * @param what the described thing (e.g. "#methodName")
   * @param spec the specification body
   */
  fun describe(what: String, spec: Specification.() -> Unit) {
    builder += DynamicContainer.dynamicContainer(
      what,
      mutableListOf<DynamicNode>().also {
        Specification(it, this@Specification).spec()
      }.toList()
    )
  }

  /**
   * Define a subspecification for a particular situation
   * @param case the described situation (e.g. "when X is empty")
   * @param spec the specification body
   */
  fun context(case: String, spec: Specification.() -> Unit) = describe(case, spec)

  /**
   * Define a specification example
   */
  fun it(description: String, body: TestInstance.() -> Unit) {
    builder += DynamicTest.dynamicTest(description) {
      TestInstance(this).also { instance ->
        instance.body()
        for (support in supports) {
          support.cleanup(instance)
        }
      }
    }
  }

  /**
   * Define a [refine]able supporting collaborator, which will be instantiated exactly once for each test instance.
   * @param destructor is called once after test finishes with the refined value
   */
  fun <T> support(destructor: (TestInstance.(T) -> Unit)? = null, constructor: TestInstance.() -> T) =
    (Collaborator<T>(constructor, destructor).also { supports.add(it) } as TestInstance.() -> T)

  /**
   * Define a [refine]able subject under test, which will be instantiated exactly once for each test instance.
   * @param destructor is called once after test finishes with the refined value
   */
  fun <T> subject(destructor: (TestInstance.(T) -> Unit)? = null, constructor: TestInstance.() -> T) = support(destructor, constructor)

  /**
   * Refine a collaborator by replacing the original definition with the result of the refinement
   */
  fun <T> refine(collaborator: TestInstance.() -> T, refinement: TestInstance.(T) -> T) {
    if (collaborator is Collaborator<T>) {
      collaborator.refine(this, refinement)
    } else {
      throw IllegalArgumentException("Only support and subject collaborators can be refined, not $collaborator")
    }
  }
}

class TestInstance internal constructor(internal val context: Specification) {
  /**
   * Ensure the collaborator is evaluated
   */
  fun <T> force(collaborator: TestInstance.() -> T) {
    this.collaborator()
  }
  /**
   * Ensure the collaborator is evalated
   */
  fun <T> force(@Suppress("UNUSED_PARAMETER") collaborator: T) { }

  @Deprecated(message = "describe inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun describe(what: String, spec: Specification.() -> Unit) { }

  @Deprecated(message = "context inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun context(case: String, spec: Specification.() -> Unit) { }

  @Deprecated(message = "it inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun it(description: String, body: TestInstance.() -> Unit) { }

  @Deprecated(message = "support inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun <T> support(destructor: (TestInstance.(T) -> Unit)? = null, constructor: TestInstance.() -> T) { }

  @Deprecated(message = "subject inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun <T> subject(destructor: (TestInstance.(T) -> Unit)? = null, constructor: TestInstance.() -> T) { }

  @Deprecated(message = "refine inside instance example not allowed", level = DeprecationLevel.ERROR)
  @Suppress("UNUSED_PARAMETER")
  fun <T> refine(collaborator: TestInstance.() -> T, refinement: TestInstance.(T) -> T) { }
}

internal class Collaborator<T>(
  private val rootInitializer: TestInstance.() -> T,
  private val destructor: (TestInstance.(T) -> Unit)?
): (TestInstance) -> T {
  private val refinements = ConcurrentHashMap<Specification, MutableList<TestInstance.(T) -> T>>()
  private val memo = ConcurrentHashMap<TestInstance, Optional<T>>()

  override fun invoke(thisRef: TestInstance): T {
    return memo.computeIfAbsent(thisRef) {
      Optional.ofNullable(
        thisRef.context.ancestors.fold(thisRef.rootInitializer()) { acc, ancestor ->
          refinements[ancestor]?.fold(acc) { acc2, refinement -> thisRef.refinement(acc2) } ?: acc
        }
      )
    }.orElse(null)
  }

  internal fun refine(context: Specification, refinement: TestInstance.(old: T) -> T) {
    refinements.computeIfAbsent(context) { mutableListOf() }.add(refinement)
  }

  internal fun cleanup(instance: TestInstance) {
    memo.remove(instance)?.let {
      val value: T = it.orElse(null)
      destructor?.invoke(instance, value) ?: if (value is AutoCloseable) value.close()
    }
  }
}
