Guidebook
=========

Guidebook is an extension library and style guide for [Scalatest Selenium DSL](http://www.scalatest.org/user_guide/using_selenium)
for even simpler regression testing and producing a guidebook-style report for your tests.

Usage
-----
1. Add the following to your build.sbt

```
libraryDependencies ++= Seq(
  "com.github.wongelz"           %% "guidebook"                % "0.0.1-SNAPSHOT"  % Test
)

testOptions in Test ++= Seq(Tests.Argument(TestFrameworks.ScalaTest, "-o"), Tests.Argument(TestFrameworks.ScalaTest, "-C", "com.wongelz.guidebook.GuidebookReporter"))
```

(Optional) If parallel browser windows (suite executions) are causing problems

```
parallelExecution in Test := false

```

2. Your test classes should extend `Guidebook`
