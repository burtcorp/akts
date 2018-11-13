package io.burt.akts

import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

internal class Collaborator<T>(
  private val rootInitializer: TestContext.() -> T,
  private val destructor: (TestContext.(T) -> Unit)?
): (TestContext) -> T {
  private val refinements = ConcurrentHashMap<SpecificationDsl, MutableList<TestContext.(T) -> T>>()
  private val memo = ConcurrentHashMap<TestContext, Optional<T>>()

  override fun invoke(thisRef: TestContext): T {
    return memo.computeIfAbsent(thisRef) {
      Optional.ofNullable(
        thisRef.context.ancestors.fold(thisRef.rootInitializer()) { acc, ancestor ->
          refinements[ancestor]?.fold(acc) { acc2, refinement -> thisRef.refinement(acc2) } ?: acc
        }
      )
    }.orElse(null)
  }

  internal fun refine(context: SpecificationDsl, refinement: TestContext.(old: T) -> T) {
    refinements.computeIfAbsent(context) { mutableListOf() }.add(refinement)
  }

  internal fun cleanup(context: TestContext) {
    memo.remove(context)?.let {
      val value: T = it.orElse(null)
      destructor?.invoke(context, value) ?: if (value is AutoCloseable) value.close()
    }
  }
}
