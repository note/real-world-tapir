import com.softwaremill.SbtSoftwareMillCommon.commonSmlBuildSettings
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt.{Project, TestFramework}

object Common {
  implicit class ProjectFrom(project: Project) {
    def commonSettings(nameArg: String, versionArg: String): Project = project.settings(
      name := nameArg,
      organization := "pl.msitko",
      version := versionArg,

      scalaVersion := "3.3.0-RC2",
      scalafmtOnCompile := true,

      // TODO: otherwise getting "method schemaForCaseClass is declared as `inline`, but was not inlined"
      scalacOptions ++= Seq("-Xmax-inlines", "64", "-Wunused:all"),

      commonSmlBuildSettings,
      testFrameworks += new TestFramework("munit.Framework")
    )
  }
}
