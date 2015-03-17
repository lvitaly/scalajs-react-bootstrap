import sbt._
import Keys._

import org.scalajs.sbtplugin.ScalaJSPlugin
import ScalaJSPlugin._
import ScalaJSPlugin.autoImport._

object Settings {

  object versions {
    val scala = "2.11.5"
    val scalajsReact = "0.8.1"
  }

}

object ScalajsReactBootstrap extends Build {

  def commonSettings: Project => Project =
    _.enablePlugins(ScalaJSPlugin)
      .settings(
        organization := "com.acework",
        version := "0.0.1-SNAPSHOT",
        scalaVersion := Settings.versions.scala,
        scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature",
          "-language:postfixOps", "-language:implicitConversions",
          "-language:higherKinds", "-language:existentials")
      )

  def preventPublication: Project => Project =
    _.settings(
      publishArtifact := false)

  def publicationSettings: Project => Project =
    _.settings(
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
      }
    )

  def utestSettings: Project => Project =
    _.configure(useReactJs("test"))
      .settings(
        libraryDependencies ++= Seq(
          "com.lihaoyi" %%% "utest" % "0.3.0"
          , "com.github.japgolly.scalajs-react" %%% "test" % "0.8.2" % "test"
        )
        , testFrameworks += new TestFramework("utest.runner.Framework")
        , scalaJSStage in Test := FastOptStage
        , requiresDOM := true
        , jsEnv in Test := PhantomJSEnv().value
      )

  def useReactJs(scope: String = "compile"): Project => Project =
    _.settings(
      jsDependencies ++= Seq(
        "org.webjars" % "react" % "0.12.1" % scope / "react-with-addons.js" commonJSName "React"
        , "org.webjars" % "log4javascript" % "1.4.10" / "js/log4javascript_uncompressed.js"
      )
      , skip in packageJSDependencies := false
    )

  lazy val root = Project("root", file("."))
    .aggregate(core, test, demo)

  lazy val core = project
    .configure(commonSettings, publicationSettings)
    .settings(
      name := "core",
      emitSourceMaps := true,
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.8.0"
        , "com.github.japgolly.scalajs-react" %%% "core" % Settings.versions.scalajsReact
        , "com.github.japgolly.scalajs-react" %%% "extra" % Settings.versions.scalajsReact
      ))

  lazy val test = project
    .configure(commonSettings, publicationSettings, utestSettings)
    .dependsOn(core)
    .settings(
      name := "test",
      scalacOptions += "-language:reflectiveCalls")

  lazy val demo = Project("demo", file("demo"))
    .dependsOn(core)
    .configure(commonSettings, useReactJs(), preventPublication)
    .settings(
      emitSourceMaps := true,
      artifactPath in(Compile, fullOptJS) := file("demo/res/demo.js")
    )
}
