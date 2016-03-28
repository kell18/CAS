organization  := "edu.kpfu.itis"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.11.7"

 libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.10.0"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-client"  % sprayV,
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-json"    % "1.3.1",
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV % "test",
    "org.specs2"          %% "specs2-core"    % "2.4.17" % "test",
    "org.specs2"          %% "specs2-junit"   % "2.4.17" % "test"
  )
}

Revolver.settings
