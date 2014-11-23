import com.typesafe.sbt.SbtNativePackager.packageArchetype

packageArchetype.java_server

packageSummary := """The Datahopper Harvester Service"""

packageDescription := """The DataHopper Harvester Service"""

maintainer := "Ian Murray <ian@sprily.co.uk>"

name := "dh-harvester"

debianPackageDependencies in Debian ++= Seq("java2-runtime")

val Snapshot       = """^(\d+\.\d+\.\d+)-SNAPSHOT$""".r

val WithQuantifier = """^(\d+\.\d+\.\d+)-(.*)$""".r

val NoQuantifier   = """^(\d+\.\d+\.\d+)$""".r

val buildCount = SettingKey[Option[String]]("buildCount", "Used as the least significant build version number, eg the debian_revision")

val commitId = SettingKey[Option[String]]("commitId", "Used to identify the commit that this package was built from.")

buildCount := Option(System.getenv().get("GO_PIPELINE_COUNTER"))

commitId := Option(System.getenv().get("GO_REVISION"))

version in Debian <<= (version, buildCount, commitId) { (v, build, commit) =>
  val buildStr = build.map("-" + _).getOrElse("")
  val commitStr = commit.map(_.take(8)).map("~" + _).getOrElse("")
  v match {
    case Snapshot(v)         => v + "~~" + "SNAPSHOT" + buildStr + commitStr
    case WithQuantifier(v,q) => v + "~" + q + "-" + buildStr
    case NoQuantifier(v)     => v + "-" + buildStr
  }
}
