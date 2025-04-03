import Dependencies._
import sbtassembly.AssemblyPlugin.defaultShellScript

lazy val projectName = "ChatFormatter"
lazy val orgName = "org.winlogon"
lazy val mainScalaClass = s"$orgName.chatformatter.$projectName"
lazy val buildScalaVersion = "3.3.5"

ThisBuild / scalaVersion     := buildScalaVersion
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := orgName
ThisBuild / organizationName := "winlogon"
Compile / mainClass := Some(mainScalaClass)

// GitHub CI
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))
ThisBuild / publishTo := None
publish / skip := true

crossScalaVersions := Seq(buildScalaVersion)

lazy val root = (project in file("."))
  .settings(
    name := projectName,
    assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(false),
  )

// Merge strategy for avoiding conflicts in dependencies
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

assembly / mainClass := Some(mainScalaClass)

libraryDependencies ++= Seq(
  "io.papermc.paper" % "paper-api" % "1.21.4-R0.1-SNAPSHOT" % Provided,
  "net.luckperms" % "api" % "5.4" % Provided,
  "dev.jorel" % "commandapi-bukkit-core" % "9.7.0" % Provided,
  "net.kyori" % "adventure-text-minimessage" % "4.18.0" % Provided,
)

resolvers ++= Seq(
  "papermc-repo" at "https://repo.papermc.io/repository/maven-public/",
  "codemc" at "https://repo.codemc.org/repository/maven-public/",
)
