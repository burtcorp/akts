package io.burt.akts

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Create a new test factory describing the specified class
 *
 * @param T the described class type
 * @param spec the specification body
 */
inline fun <reified T> describe(noinline spec: SpecificationDsl.() -> Unit) = describe(T::class.java, spec)

/**
 * Create a new test factory describing the specified class
 *
 * @param T the described class type
 * @param type the described class instance
 * @param spec the specification body
 */
fun <T> describe(type: Class<T>, spec: SpecificationDsl.() -> Unit) : Stream<DynamicNode> =
  describe(type, Options.isFlatNamespace, spec)

internal fun <T> describe(type: Class<T>, isFlatNamespace: Boolean = Options.isFlatNamespace, spec: SpecificationDsl.() -> Unit) : Stream<DynamicNode> =
  sequenceOf(DynamicContainer.dynamicContainer(
    type.simpleName,
    null,
    mutableListOf<DynamicNode>().also {
      SpecificationDsl.Root(it, type.simpleName, isFlatNamespace).spec()
    }.asSequence().asStream()
  )).asStream()
