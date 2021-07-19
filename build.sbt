name := "Zen Search"
 
version := "1.0"
 
scalaVersion := "2.12.14"
 
resolvers += "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
 
val akka_version = "2.6.15"

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-actor"        % akka_version,
  "com.typesafe.akka"   %% "akka-testkit"      % akka_version % "it,test",
  "io.spray"            %  "spray-json_2.12"   % "1.3.6",
  "org.scalatest"       %% "scalatest"         % "3.2.9" % "it,test",
  "org.wvlet.airframe"  %% "airframe-launcher" % "19.11.1"
)

logLevel := Level.Info

lazy val zensearch = (project in file(".")).
  configs(IntegrationTest).settings(Defaults.itSettings:_*)


