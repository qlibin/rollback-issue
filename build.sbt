// NOTE
// RESIST THE TEMPATION TO DO A LOT OF STUFF IN THIS FILE.
// SBT IS COOL. BUT IT CAN BECOME A DEAD-COMPLICATED AND UN-MAINTAINABLE MESS
// IF YOU BLOAT THE BUILD.SBT.
//
// IF YOU ADD EVEN A SINGLE LINE: ASK YOURSELF: IS THERE A DIFFERENT WAY
// TO ACHIEVE THIS?
name := """test-app"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.11"

libraryDependencies += jdbc
libraryDependencies += evolutions
libraryDependencies += cache
libraryDependencies += ws
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.5.3"
libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1212"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test
libraryDependencies += "com.opentable.components" % "otj-pg-embedded" % "0.7.1" % Test

libraryDependencies += "com.storm-enroute" %% "scalameter-core" % "0.6"

// Show full stacktraces when tests fail. Makes debugging more easy...
testOptions in Test += Tests.Argument("-oF")

javaOptions in Test += "-Dlogger.file=conf/logback-test.xml"
javaOptions in Test += "-Dconfig.file=conf/application-test.conf"
