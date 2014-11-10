import com.typesafe.sbt.SbtNativePackager.packageArchetype

rpmVendor in Rpm := "Sprily IT Ltd."

rpmUrl in Rpm := Some("http://github.com.sprily/dh-harvester")

rpmRelease in Rpm := "1"

rpmLicense in Rpm := Some("GPLv3")

packageArchetype.java_server
