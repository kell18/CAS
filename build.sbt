organization  := "edu.kpfu.itis"

version       := "0.1"

scalaVersion  := "2.10.5"

scalaSource in Compile := baseDirectory.value / "src"

scalaSource in Test := baseDirectory.value / "specs" / "tests"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.10.6"
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.1"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-json"    % "1.3.1",
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.specs2"          %%  "specs2-core"   % "2.3.7" % "test"
  )
}

Revolver.settings
