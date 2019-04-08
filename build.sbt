import sbt._
import sbt.Keys._
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.sbt.pgp.PgpKeys
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

val Org = "org.scoverage"
val MockitoVersion = "2.19.0"
val ScalatestVersion = "3.0.6-SNAP4"

val appSettings = Seq(
    organization := Org,
    scalaVersion := "2.12.7",
    crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.7", "2.13.0-M5"),
    fork in Test := false,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    parallelExecution in Test := false,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8"),
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    publishTo := Some("Artifactory Realm" at "https://domino.jfrog.io/domino/domino-open-source"),
    credentials += Credentials("Artifactory Realm", "domino.jfrog.io", "username", "password"),
    pomExtra := {
      <url>https://github.com/scoverage/scalac-scoverage-plugin</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:scoverage/scalac-scoverage-plugin.git</url>
          <connection>scm:git@github.com:scoverage/scalac-scoverage-plugin.git</connection>
        </scm>
        <developers>
          <developer>
            <id>sksamuel</id>
            <name>Stephen Samuel</name>
            <url>http://github.com/sksamuel</url>
          </developer>
        </developers>
    },
    pomIncludeRepository := {
      _ => false
    }
  ) ++ Seq(
    releaseCrossBuild := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value
  )

lazy val root = Project("scalac-scoverage", file("."))
    .settings(name := "scalac-scoverage")
    .settings(appSettings: _*)
    .settings(publishArtifact := false)
    .settings(publishLocal := {})
    .aggregate(plugin, runtime.jvm, runtime.js)

lazy val runtime = CrossProject("scalac-scoverage-runtime", file("scalac-scoverage-runtime"))(JVMPlatform, JSPlatform)
    .crossType(CrossType.Full)
    .settings(name := "scalac-scoverage-runtime_domino")
    .settings(appSettings: _*)
    .jvmSettings(
      libraryDependencies ++= Seq(
      "org.mockito" % "mockito-core" % MockitoVersion % "test",
      "org.scalatest" %% "scalatest" % ScalatestVersion % "test"
      )
    )
    .jsSettings(
      libraryDependencies += "org.scalatest" %%% "scalatest" % ScalatestVersion % "test",
      scalaJSStage := FastOptStage
    )

lazy val `scalac-scoverage-runtimeJVM` = runtime.jvm
lazy val `scalac-scoverage-runtimeJS` = runtime.js

lazy val plugin = Project("scalac-scoverage-plugin", file("scalac-scoverage-plugin"))
    .dependsOn(`scalac-scoverage-runtimeJVM` % "test")
    .settings(name := "scalac-scoverage-plugin_domino")
    .settings(appSettings: _*)
    .settings(libraryDependencies ++= Seq(
    "org.mockito" % "mockito-core" % MockitoVersion % "test",
    "org.scalatest" %% "scalatest" % ScalatestVersion % "test",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
  )).settings(libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor > 10 => Seq(
        "org.scala-lang.modules" %% "scala-xml" % "1.1.1",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0" % "test"
      )
      case _ => Seq(
        "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2" % "test"
      )
    }
  })
