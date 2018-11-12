package io.burt.akts

import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.fileExtension
import kotlin.streams.toList

/**
 * Base class for specifications constructed from `a.kts` scripts
 */
@KotlinScript(
  fileExtension = "a.kts",
  compilationConfiguration = AktsScript.CompilationConfiguration::class
)
abstract class AktsScript {
  private val examples = mutableListOf<DynamicNode>()

  /**
   * Add a specification for the given type
   *
   * @param T the described class type
   * @param spec the specification body
   */
  inline fun <reified T> describe(noinline spec: SpecificationDsl.() -> Unit) = describe(T::class.java, spec)

  /**
   * Add a specification for the given type
   *
   * @param T the described class type
   * @param type the described class instance
   * @param spec the specification body
   */
  fun <T> describe(type: Class<T>, spec: SpecificationDsl.() -> Unit) =
    describe(type, Options.isFlatNamespace, spec)

  internal fun <T> describe(type: Class<T>, isFlatNamespace: Boolean = Options.isFlatNamespace, spec: SpecificationDsl.() -> Unit) {
    examples += io.burt.akts.describe(type, isFlatNamespace, spec).toList()
  }

  @TestFactory
  fun tests(): Iterable<DynamicNode> = examples

  internal class CompilationConfiguration: ScriptCompilationConfiguration({
    fileExtension("a.kts")
    baseClass(AktsScript::class)
  })
}

