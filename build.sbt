import NativePackagerHelper._
import sbtrelease.ReleaseStateTransformations._

name := "entellect-topic-subset-generator"

version := "0.1"

scalaVersion := "2.12.10"

lazy val kafkaVersion = "2.4.0"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.0",
  "org.apache.kafka" %% "kafka" % kafkaVersion,
  "org.apache.kafka" % "kafka-streams" % kafkaVersion,
  "org.apache.kafka" %% "kafka-streams-scala" % kafkaVersion,
  "net.liftweb" %% "lift-json" % "3.4.2",
  "org.json4s" %% "json4s-native" % "3.7.0-M6",
  "io.github.embeddedkafka" %% "embedded-kafka-streams" % "2.4.0" % "test",
)
val banana = (name: String) => "org.w3" %% name % "0.8.4" excludeAll (ExclusionRule(organization = "org.scala-stm"))

//add the bblfish-snapshots repository to the resolvers

resolvers += "bblfish-snapshots" at "http://bblfish.net/work/repo/releases"
//choose the packages you need for your dependencies
val bananaDeps = Seq("banana", "banana-rdf", "banana-rdf4j").map(banana)


def dockerImage(version: String): Seq[ImageName] = {
  val imageName = ImageName(
    namespace = sys.env.get("DOCKER_REGISTRY"),
    repository = sys.env.getOrElse("DOCKER_REPOSITORY", "undefined"),
    tag = Some(version)
  )
  Seq(imageName, imageName.copy(tag = Some("latest")))
}


lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    // other settings here
  )
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(dockerfile in docker := {

    val fatJar: sbt.File = (assemblyOutputPath in assembly).value
    val fatJarTargetPath = s"/app/${name.value}-assembly.jar"
    val appDir: File     = stage.value
    val targetDir        = "/app"
    new Dockerfile {
      from("adoptopenjdk/openjdk11:jdk-11.28")
      add(fatJar, fatJarTargetPath)
    }
  })
  .settings(imageNames in docker := dockerImage(version.value), docker := docker.dependsOn(assembly).value)
  .settings( credentials += Credentials("Artifactory Realm", "artifactory.dev.elssie.cm-elsevier.com", "sbtuser", "wibblewobble"),
    publishTo := Some("Artifactory Realm" at "http://artifactory.dev.elssie.cm-elsevier.com/artifactory/releases"),
  )

releaseTagComment := s"[skip ci] Releasing ${(version in ThisBuild).value}"
releaseCommitMessage := s"[skip ci] Setting version to ${(version in ThisBuild).value}"
releaseNextCommitMessage := s"[skip ci] Setting version to ${(version in ThisBuild).value}"

val meta = """META.INF(.)*""".r
val metaServices = """META.INF/services/(.)+""".r

assemblyMergeStrategy in assembly := {
  case "application.conf" => MergeStrategy.concat
  case "reference.conf" => MergeStrategy.concat
  case PathList("org", "apache", "commons", _@_*) => MergeStrategy.last
  case PathList("org", "joda", "time", _@_*) => MergeStrategy.last
  case metaServices(_) => MergeStrategy.concat
  case meta(_) => MergeStrategy.discard
  case _ => MergeStrategy.first
}


enablePlugins(_root_.com.typesafe.sbt.packager.docker.DockerPlugin)
enablePlugins(JavaAppPackaging)