lazy val projectName = "Glyphide"
lazy val orgName = "org.winlogon"
lazy val mainScalaClass = s"$orgName.glyphide.GlyphideLoader"
lazy val buildScalaVersion = "3.7.4"
lazy val minecraft = "26.1.2.build.+"

ThisBuild / scalaVersion     := buildScalaVersion
ThisBuild / version          := "0.5.0"
ThisBuild / organization     := orgName
ThisBuild / organizationName := "winlogon"
Compile / mainClass := Some(mainScalaClass)

// MockBukkit's plugin classloader only sees the *system* classpath, so in-process testing hides
// plugin classes from it, showing a "No jar file selected" error. We fork, like Gradle does.
Test / fork := true

lazy val root = (project in file("."))
    .settings(
        name := projectName,
        assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(false)
    )

// Merge strategy for avoiding conflicts in dependencies
assembly / assemblyMergeStrategy := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case _                             => MergeStrategy.first
}

assembly / mainClass := Some(mainScalaClass)

libraryDependencies ++= Seq(
    "io.papermc.paper" % "paper-api" % s"$minecraft" % Provided,
    "net.luckperms" % "api" % "5.5" % Provided,
    "org.unbescape" % "unbescape" % "1.1.6.RELEASE" % Provided,
    "org.winlogon" % "retrohue" % "0.2.0" % Provided,

    // testing
    "org.mockbukkit.mockbukkit" % "mockbukkit-v26.1.2" % "4.115.0" % Test,
    "org.mockito" % "mockito-core" % "5.23.0" % Test,
    "io.papermc.paper" % "paper-api" % s"$minecraft" % Test,

    // junit jupiter
    "com.github.sbt.junit" % "jupiter-interface" % "0.19.0" % Test
)

resolvers ++= Seq(
    "papermc-repo" at "https://repo.papermc.io/repository/maven-public/",
    "codemc" at "https://repo.codemc.org/repository/maven-public/",
    "winlogon-code" at "https://maven.winlogon.org/releases"
)
