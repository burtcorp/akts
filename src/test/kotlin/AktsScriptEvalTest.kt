package io.burt.akts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.javaHome
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

class AktsScriptEvalTest {
  fun evalAkts(script: String): AktsScript {
    val evaluationResult = BasicJvmScriptingHost().eval(
      script.toScriptSource(),
      createJvmCompilationConfigurationFromTemplate<AktsScript>() {
        jvm {
          javaHome(File(System.getenv("JAVA_HOME")))
        }
        dependencies.append(
          JvmDependency(
            classpathFromClassloader(Thread.currentThread().contextClassLoader).orEmpty().filter {
              it.path.contains("/akts/")
            }
          )
        )
      },
      null
    )

    when (evaluationResult) {
      is ResultWithDiagnostics.Success -> {
        val returnValue = evaluationResult.value.returnValue as ResultValue.Value
        return returnValue.value as AktsScript
      }
      is ResultWithDiagnostics.Failure -> {
        println(evaluationResult.reports)
        evaluationResult.reports.firstOrNull()?.exception?.printStackTrace()
        throw AssertionFailedError("Expected akts script evaluation to succeed")
      }
    }
  }

  @Test
  fun `#tests contains a container node for each registered describe block`() {
    val instance = evalAkts("describe<Any>() {}; describe<String>() {}")
    assertEquals(
      listOf("Object", "String"),
      instance.tests().map { it.displayName }.toList()
    )
  }
}
