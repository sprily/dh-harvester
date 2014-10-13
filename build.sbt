name := "dh-harvester"

version in ThisBuild := "0.0.1"

scalaVersion in ThisBuild := "2.11.2"

scalacOptions in ThisBuild ++= Seq(
	"-feature",
	"-deprecation",
	"-Xlint",
	"-unchecked")

lazy val core = project in file("core")

libraryDependencies in ThisBuild += "org.specs2" %% "specs2-core" % "2.4.6" % "test"

libraryDependencies in ThisBuild += "org.specs2" %% "specs2-scalacheck" % "2.4.6-scalaz-7.0.6"
