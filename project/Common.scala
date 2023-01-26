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

      scalaVersion := "3.2.1",
      scalafmtOnCompile := true,

      commonSmlBuildSettings,
      testFrameworks += new TestFramework("munit.Framework")
    )
  }
}
