package io.burt.akts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import kotlin.streams.toList

class SpecTest {
  @Test
  fun `#describe returns single container node`() {
    assertEquals(listOf(DynamicContainer::class.java), describe<Any> { }.map{ it.javaClass }.toList())
  }

  @Test
  fun `#describe names container after class name`() {
    assertEquals("SpecTest", describe<SpecTest> {  }.findFirst().get().displayName)
  }

  @Test
  fun `#describe adds inner describe and context to children`() {
    val root = describe<Any> {
      describe("hello") {
        describe("noo") { }
      }
      context("world") {
      }
    }.findFirst().get() as DynamicContainer
    assertEquals(listOf("hello", "world"), root.children.map { it.displayName }.toList())
  }

  @Test
  fun `#describe adds nested inner describe and context to nested children`() {
    val root = describe<Any> {
      describe("hello") {
        describe("noo") { }
        context("world") { }
      }
    }.findFirst().get() as DynamicContainer
    val child = root.children.findFirst().get() as DynamicContainer
    assertEquals(listOf("noo", "world"), child.children.map { it.displayName }.toList())
  }

  @Test
  fun `#describe adds it examples to list`() {
    val root = describe<Any> {
      it("does") { }
      it("stuff") { }
    }.findFirst().get() as DynamicContainer

    assertEquals(listOf("does", "stuff"), root.children.map { it.displayName }.toList())
  }

  @Test
  fun `#describe returns examples as test`() {
    val root = describe<Any> {
      it("stuff") { }
    }.findFirst().get() as DynamicContainer
    assertEquals(DynamicTest::class.java, root.children.findFirst().get().javaClass)
  }

  @Test
  fun `#describe returns tests that run the examples`() {
    var runs = 0
    val root = describe<Any> {
      it("stuff") { runs += 1 }
    }.findFirst().get() as DynamicContainer
    val test = root.children.findFirst().get() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
  }

  @Test
  fun `#describe injects supports and subjects into examples`() {
    var runs = 0
    val root = describe<Any> {
      val x = support { runs += 1 }
      val y = subject { runs += 2 }
      it("stuff") { y(); x() }
    }.findFirst().get() as DynamicContainer
    val test = root.children.findFirst().get() as DynamicTest
    test.executable.execute()
    assertEquals(3, runs)
  }

  @Test
  fun `#describe caches supports in each example`() {
    var runs = 0
    val root = describe<Any> {
      val x = support { runs += 1}
      it("stuff") { x(); x() }
    }.findFirst().get() as DynamicContainer
    val test = root.children.findFirst().get() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
  }

  @Test
  fun `#describe does not cache supports between examples`() {
    var runs = 0
    val root = describe<Any> {
      val x = support { runs += 1}
      it("does") { x() }
      it("stuff") { x() }
    }.findFirst().get() as DynamicContainer
    val tests = root.children.map { it as DynamicTest }.toList()
    tests.forEach { test -> test.executable.execute() }
    assertEquals(2, runs)
  }

  @Test
  fun `#describe supports refinements`() {
    var runs = 0
    val root = describe<Any> {
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
  fun `#describe cannot refine non-collaborators`() {
    try {
      val root = describe<Any> {
        val x = { x: TestInstance -> }
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
  fun `#describe triggers only refinements in scope`() {
    var runs = 0
    val root = describe<Any> {
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
  fun `#describe allows forcing supports`() {
    var runs = 0
    val root = describe<Any> {
      val x = support { runs += 1}
      it("stuff") { force(x) }
    }.findFirst().get() as DynamicContainer
    val test = root.children.toList().first() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
  }

  @Test
  fun `#describe allows forcing already forced supports`() {
    var runs = 0
    val root = describe<Any> {
      val x = support { runs += 1}
      it("stuff") { force(x()) }
    }.findFirst().get() as DynamicContainer
    val test = root.children.toList().first() as DynamicTest
    test.executable.execute()
    assertEquals(1, runs)
  }
}
