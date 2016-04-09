organization  := "edu.kpfu.itis"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.7"

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.10.0"


libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.6"

libraryDependencies += "net.java.dev.jna" % "jna" % "4.1.0"

libraryDependencies += "com.github.spullara.mustache.java" % "compiler" % "0.9.0"

libraryDependencies += "org.elasticsearch" % "elasticsearch" % "2.3.0"

libraryDependencies += "com.sksamuel.elastic4s" % "elastic4s-core_2.11" % "2.3.0"

libraryDependencies += "com.sksamuel.elastic4s" % "elastic4s-jackson_2.11" % "2.3.0"

libraryDependencies += "com.sksamuel.elastic4s" % "elastic4s-streams_2.11" % "2.3.0"

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
//    "ch.qos.logback" % "logback-classic" % "1.1.3",
//    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
//    "org.codehaus.groovy" %%  "groovy-all"          % "1.8.2",
//    "org.apache.lucene"   %%  "lucene-expressions"  % "4.10.2",
    "io.spray"            %%  "spray-client"        % sprayV,
    "io.spray"            %%  "spray-can"           % sprayV,
    "io.spray"            %%  "spray-routing"       % sprayV,
    "io.spray"            %%  "spray-json"          % "1.3.1",
    "io.spray"            %%  "spray-testkit"       % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"          % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"        % akkaV % "test",
    "org.specs2"          %%  "specs2-core"         % "2.4.17" % "test",
    "org.specs2"          %%  "specs2-junit"        % "2.4.17" % "test"
  )
}

Revolver.settings
