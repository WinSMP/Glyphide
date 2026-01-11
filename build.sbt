lazy val projectName = "Glyphide"
lazy val orgName = "org.winlogon"
lazy val mainScalaClass = s"$orgName.glyphide.GlyphideLoader"
lazy val buildScalaVersion = "3.7.4"
lazy val minecraft = "1.21.10"

ThisBuild / scalaVersion     := buildScalaVersion
ThisBuild / version          := "0.4.0"
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
  "io.papermc.paper" % "paper-api" % s"$minecraft-R0.1-SNAPSHOT" % Provided,
  "net.luckperms" % "api" % "5.5" % Provided,
  "org.unbescape" % "unbescape" % "1.1.6.RELEASE" % Provided,
  "org.winlogon" % "retrohue" % "0.1.0" % Provided,

  // testing
  "org.mockbukkit.mockbukkit" % "mockbukkit-v1.21" % "4.99.0" % Test,
  "org.mockito" % "mockito-core" % "5.21.0" % Test,
  "io.papermc.paper" % "paper-api" % s"$minecraft-R0.1-SNAPSHOT" % Test,

  // junit jupiter
  "com.github.sbt.junit" % "jupiter-interface" % "0.17.0" % Test
)

resolvers ++= Seq(
  "papermc-repo" at "https://repo.papermc.io/repository/maven-public/",
  "codemc" at "https://repo.codemc.org/repository/maven-public/",
  "winlogon-code" at "https://maven.winlogon.org/releases",
)
