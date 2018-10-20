package io.burt.akts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.Optional
import kotlin.streams.toList

class SpecTest {
  @Test
  fun `#describe returns a single container node`() {
    assertEquals(listOf(DynamicContainer::class.java), describe(Any::class.java, false) { }.map{ it.javaClass }.toList())
  }

  @Test
  fun `#describe names the container after the class name`() {
    assertEquals("SpecTest", describe(SpecTest::class.java, false) {  }.findFirst().get().displayName)
  }

  @Test
  fun `#describe and #context inside become children of the root container`() {
    val root = describe(Any::class.java, false) {
      describe("hello") {
        describe("noo") { }
      }
      context("world") {
      }
    }.findFirst().get() as DynamicContainer
    assertEquals(listOf("hello", "world"), root.children.map { it.displayName }.toList())
  }

  @Test
  fun `#describe and #context can be nested arbitrarily deep`() {
    val root = describe(Any::class.java, false) {
      describe("hello") {
        describe("noo") { }
        context("world") { }
      }
    }.findFirst().get() as DynamicContainer
    val child = root.children.findFirst().get() as DynamicContainer
    assertEquals(listOf("noo", "world"), child.children.map { it.displayName }.toList())
  }

  @Test
  fun `#it adds an example to the corresponding #describe or #context container`() {
    val root = describe(Any::class.java, false) {
      it("does") { }
      it("stuff") { }
    }.findFirst().get() as DynamicContainer
    assertEquals(listOf("does", "stuff"), root.children.map { it.displayName }.toList())
  }

  @Test
  fun `#it includes full context description when using flat namespace`() {
    val root = describe(SpecTest::class.java, true) {
      it("does") { }
      context("noo") {
        it("stuff") { }
      }
    }.findFirst().get() as DynamicContainer
    val children = root.children.toList()
    assertEquals(listOf("SpecTest does", "noo"), children.map { it.displayName })
    val nested = children.last() as DynamicContainer
    assertEquals(listOf("SpecTest noo stuff"), nested.children.map { it.displayName }.toList())
  }

  @Test
  fun `#it sets a test source URI in flat namespace`() {
    val root = describe(Any::class.java, true) {
      it("does") { }
      context("noo") {
        it("stuff") { }
      }
    }.findFirst().get() as DynamicContainer
    val children = root.children.toList()
    assertEquals(listOf(Optional.of(URI("flat")), Optional.empty()), children.map { it.testSourceUri })
    val nested = children.last() as DynamicContainer
    assertEquals(listOf(Optional.of(URI("flat"))), nested.children.map { it.testSourceUri }.toList())
  }

  @Test
  fun `#it relies on the default test source URI when using nested namespace`() {
    val root = describe(Any::class.java, false) {
      it("does") { }
      context("noo") {
        it("stuff") { }
      }
    }.findFirst().get() as DynamicContainer
    val children = root.children.toList()
    assertEquals(listOf(Optional.empty<URI>(), Optional.empty()), children.map { it.testSourceUri })
    val nested = children.last() as DynamicContainer
    assertEquals(listOf(Optional.empty<URI>()), nested.children.map { it.testSourceUri }.toList())
  }

  @Test
  fun `#it creates a test object that runs the example body`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      it("stuff") { runs += 1 }
    }.findFirst().get() as DynamicContainer
    val test = root.children.findFirst().get() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
  }

  @Test
  fun `#support and #subject define collaborators that can be used inside examples`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support { runs += 1 }
      val y = subject { runs += 2 }
      it("stuff") { y(); x() }
    }.findFirst().get() as DynamicContainer
    val test = root.children.findFirst().get() as DynamicTest
    test.executable.execute()
    assertEquals(3, runs)
  }

  @Test
  fun `#support and #subject memoize their results`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support { runs += 1}
      it("stuff") { x(); x() }
    }.findFirst().get() as DynamicContainer
    val test = root.children.findFirst().get() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
  }

  @Test
  fun `#support and #subject does not memoize between examples`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support { runs += 1}
      it("does") { x() }
      it("stuff") { x() }
    }.findFirst().get() as DynamicContainer
    val tests = root.children.map { it as DynamicTest }.toList()
    tests.forEach { test -> test.executable.execute() }
    assertEquals(2, runs)
  }

  @Test
  fun `#refine overrides the value of a previously declared collaborator`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support { runs += 1}
      refine(x) { runs += 1 }
      refine(x) { runs += 1 }
      it("stuff") { x() }
    }.findFirst().get() as DynamicContainer
    val test = root.children.findFirst().get() as DynamicTest
    test.executable.execute()
    assertEquals(3, runs)
  }

  @Test
  fun `#refine throws IllegalArgumentException when applied to a non-collaborator`() {
    try {
      val root = describe(Any::class.java, false) {
        val x = { _: TestInstance -> }
        refine(x) { }
      }.findFirst().get() as DynamicContainer
      root.children.forEach { }
      fail<Unit>("Should have thrown")
    } catch(e: IllegalArgumentException) {
      val message = e.message?.replace("Function1<io.burt.akts.TestInstance, kotlin.Unit>", "(io.burt.akts.TestInstance) -> kotlin.Unit")
      assertEquals("Only support and subject collaborators can be refined, not (io.burt.akts.TestInstance) -> kotlin.Unit", message)
    }
  }

  @Test
  fun `#refine only affects the value of collaborators in the same, or child contexts`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support { runs += 1}
      it("stuff") { x() }
      context("two") {
        refine(x) { runs += 1 }
        refine(x) { runs += 1 }
        it("also") { x() }
      }
    }.findFirst().get() as DynamicContainer
    val children = root.children.toList()
    val test = children.first() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
    val context = children.last() as DynamicContainer
    val test2 = context.children.toList().first() as DynamicTest
    test2.executable.execute()
    assertEquals(4, runs)
  }

  @Test
  fun `#force ensures that a collaborator is evaluated`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support { runs += 1}
      it("stuff") { force(x) }
    }.findFirst().get() as DynamicContainer
    val test = root.children.toList().first() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
  }

  @Test
  fun `#force can be applied to an already evaluated collaborator`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support { runs += 1}
      it("stuff") { force(x()) }
    }.findFirst().get() as DynamicContainer
    val test = root.children.toList().first() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
  }

  @Test
  fun `#support and #subject can be declared with a destructor that runs after the tests finishes`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support({ runs += 2}) { runs += 1 }
      it("stuff") { x(); assertEquals(1, runs) }
    }.findFirst().get() as DynamicContainer
    val test = root.children.toList().first() as DynamicTest
    test.executable.execute()
    assertEquals(3, runs)
  }

  @Test
  fun `#support and #subject does not invoke the destructor unless the collaborator is evaluated`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support({ runs += 2}) { runs += 1 }
      it("stuff") { x.hashCode() }
    }.findFirst().get() as DynamicContainer
    val test = root.children.toList().first() as DynamicTest
    test.executable.execute()
    assertEquals(0, runs)
  }

  @Test
  fun `#support and #subject automatically closes an collaborator that is AutoClosable`() {
    var runs = 0
    val root = describe(Any::class.java, false) {
      val x = support { runs += 1 ;AutoCloseable { runs += 2 } }
      it("stuff") { x() }
    }.findFirst().get() as DynamicContainer
    val test = root.children.toList().first() as DynamicTest
    test.executable.execute()
    assertEquals(3, runs)
  }
}
