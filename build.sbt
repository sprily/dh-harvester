name := "dh-harvester"

version in ThisBuild := "0.0.1"

scalaVersion in ThisBuild := "2.11.2"

lazy val core = project in file("core")

libraryDependencies in ThisBuild += "org.specs2" %% "specs2-core" % "2.4.6" % "test"
