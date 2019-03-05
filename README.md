# Akts - refinable test specifications inspired by RSpec

Coming from an RSpec background, we have developed a particular testing style based on refinements of subjects and supporting actors. In this style, all collaborates are declared in the beginning, and their behavior is narrowed down in nested describe and context blocks.

This brings a similar style of testing to Kotlin, while trying to retain all static type information and with minimal amount of needless syntax.

With this implementation, a test looks something like this

```
@TestFactory
fun test() = describe<Calculator> {
  val initialState = support { 0 }
  val calculator = subject { Calculator(initialState()) }

  describe("#state") {
    it("returns the initial state") {
      assertEqual(calculator().state(), 0)
    }

    context("when initial state is negative") {
      refine(initialState) { old -> old - 1 }

      it("still returns the initial state") {
        assertEqual(calculator().state(), -1)
      }
    }
  }
}
```

## Using in your project

Development builds are currently available through [JitPack](https://jitpack.io/). Add the following to your build.gradle

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  testImplementation 'com.github.burtcorp:akts:master-SNAPSHOT'
}
```

## Copyright

© 2018 Burt AB, see LICENSE.txt (BSD 3-Clause).
