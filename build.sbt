organization  := "edu.kpfu.itis"

version       := "0.1"

scalaVersion  := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Textocat Open-Source Repository" at "http://corp.textocat.com/artifactory/oss-repo"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  val textokitV = "0.1-SNAPSHOT"
  Seq(
    "org.scala-lang"              %   "scala-reflect"                             % "2.11.7",
    "org.scala-lang.modules"      %   "scala-xml_2.11"                            % "1.0.4",
    "org.scala-lang.modules"      %   "scala-parser-combinators_2.11"             % "1.0.4",
    "com.github.nscala-time"      %%  "nscala-time"                               % "2.10.0",
    "org.apache.commons"          %   "commons-lang3"                             % "3.4",
    "org.typelevel"               %%  "cats"                                      % "0.4.1",

    "io.spray"                    %%  "spray-client"                              % sprayV,
    "io.spray"                    %%  "spray-can"                                 % sprayV,
    "io.spray"                    %%  "spray-routing"                             % sprayV,
    "io.spray"                    %%  "spray-json"                                % "1.3.1", // TODO: Migrate to 1.3.3
    "io.spray"                    %%  "spray-testkit"                             % sprayV  % "test",

    "com.typesafe.akka"           %%  "akka-actor"                                % akkaV,
    "com.typesafe.akka"           %%  "akka-testkit"                              % akkaV % "test",

    "org.specs2"                  %%  "specs2-core"                               % "2.4.17" % "test",
    "org.specs2"                  %%  "specs2-junit"                              % "2.4.17" % "test",

    "org.elasticsearch"           %   "elasticsearch"                             % "2.3.0",
    "org.ccil.cowan.tagsoup"      %   "tagsoup"                                   % "1.2",

    "com.textocat.textokit.core"  %   "textokit-lemmatizer-api"                   % textokitV,
    "com.textocat.textokit.core"  %   "textokit-tokenizer-simple"                 % textokitV % "runtime",
    "com.textocat.textokit.core"  %   "textokit-sentence-splitter-heuristic"      % textokitV % "runtime",
    "com.textocat.textokit.core"  %   "textokit-morph-dictionary-opencorpora"     % textokitV % "runtime",
    "com.textocat.textokit.core"  %   "textokit-pos-tagger-opennlp"               % textokitV % "runtime",
    "com.textocat.textokit.core"  %   "textokit-lemmatizer-dictionary-sim"        % textokitV % "runtime",
    // textokit-dictionary-opencorpora-resource and textokit-pos-tagger-opennlp-model in /lib

    "ch.qos.logback"              %   "logback-classic"                           % "1.1.6",
    "com.typesafe.scala-logging"  %%  "scala-logging"                             % "3.1.0"
  )
}

Revolver.settings
