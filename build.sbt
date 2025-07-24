import Dependencies._
import sbtassembly.AssemblyPlugin.defaultShellScript

lazy val projectName = "Glyphide"
lazy val orgName = "org.winlogon"
lazy val mainScalaClass = s"$orgName.glyphide.$projectName"
lazy val buildScalaVersion = "3.3.6"

ThisBuild / scalaVersion     := buildScalaVersion
ThisBuild / version          := "0.3.0"
ThisBuild / organization     := orgName
ThisBuild / organizationName := "winlogon"
Compile / mainClass := Some(mainScalaClass)

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
  "io.papermc.paper" % "paper-api" % "1.21.6-R0.1-SNAPSHOT" % Provided,
  "net.luckperms" % "api" % "5.4" % Provided,
  "dev.jorel" % "commandapi-bukkit-core" % "10.1.0" % Provided,
  "org.unbescape" % "unbescape" % "1.1.6.RELEASE" % Provided,
  "org.winlogon" % "retrohue" % "0.1.0" % Provided,

  // testing
  // "com.github.MockBukkit" % "MockBukkit" % "v1.21-SNAPSHOT" % Test,
  "org.mockbukkit.mockbukkit" % "mockbukkit-v1.21" % "v4.52.0" % Test,
  "org.mockito" % "mockito-core" % "5.18.0" % Test,
  "io.papermc.paper" % "paper-api" % "1.21.6-R0.1-SNAPSHOT" % Test,

  // junit jupiter
  "com.github.sbt.junit" % "jupiter-interface" % "0.15.0" % Test
)

resolvers ++= Seq(
  "papermc-repo" at "https://repo.papermc.io/repository/maven-public/",
  "codemc" at "https://repo.codemc.org/repository/maven-public/",
  "winlogon-code" at "https://maven.winlogon.org/releases",
)
