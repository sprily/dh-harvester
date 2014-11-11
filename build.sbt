organization in ThisBuild := "uk.co.sprily"

name := "dh-harvester"

version in ThisBuild := "0.0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.2"

scalacOptions in ThisBuild ++= Seq(
	"-feature",
	"-deprecation",
	"-Xlint",
	"-unchecked")

lazy val core = project in file("core")

lazy val harvester = project in file("harvester") dependsOn(core)

libraryDependencies in ThisBuild += "org.specs2" %% "specs2-core" % "2.4.6" % "test"

libraryDependencies in ThisBuild += "org.specs2" %% "specs2-scalacheck" % "2.4.6-scalaz-7.0.6"

resolvers in ThisBuild += "Sprily 3rd Party" at "http://repo.sprily.co.uk/nexus/content/repositories/thirdparty"

resolvers in ThisBuild += "Sprily Snapshots" at "http://repo.sprily.co.uk/nexus/content/repositories/snapshots"
