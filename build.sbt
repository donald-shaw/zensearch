name := "Zen Search"
 
version := "1.0"
 
scalaVersion := "2.12.2"
 
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
val akka_version = "2.5.3" 

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"      % akka_version,
  "com.typesafe.akka" %% "akka-testkit"    % akka_version % "it,test",
  "io.spray"          %  "spray-json_2.12" % "1.3.2",
  "org.scalatest"     %% "scalatest"       % "3.0.1" % "it,test"
)

logLevel := Level.Info

fork in run := true

initialCommands in console := """
         import org.zensearch.main.ZenSearchCli._

         start
    """

lazy val zensearch = (project in file(".")).
  configs(IntegrationTest).settings(Defaults.itSettings:_*)


