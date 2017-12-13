name := "calm3"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.11"
//libraryDependencies += "org.gbz" % "utils_2.12" % "0.1.2-SNAPSHOT"

libraryDependencies += "com.lihaoyi" %% "utest" % "0.6.0" % "test"
testFrameworks += new TestFramework("utest.runner.Framework")

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.0.0"

lazy val core = RootProject(file("../utils"))
val main = Project(id = "application", base = file(".")).dependsOn(core)