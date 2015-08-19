name := """twitter-clone"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  //jdbc,
  //cache,
  //ws,
  //specs2 % Test
//  "com.sksamuel.elastic4s"      %%  "elastic4s"             % "1.5.6"
//    excludeAll(
//    ExclusionRule(organization = "org.scala-lang", name = "scala-library"),
//    //      ExclusionRule(organization = "org.apache.lucene", name = "lucene-analyzers-common"),
//    ExclusionRule(organization = "org.apache.lucene", name = "lucene-highlighter"),
//    ExclusionRule(organization = "org.apache.lucene", name = "lucene-grouping"),
//    ExclusionRule(organization = "org.apache.lucene", name = "lucene-join"),
//    ExclusionRule(organization = "org.apache.lucene", name = "lucene-memory"),
//    ExclusionRule(organization = "org.apache.lucene", name = "lucene-misc"),
//    //      ExclusionRule(organization = "org.apache.lucene", name = "lucene-queries"),
//    //      ExclusionRule(organization = "org.apache.lucene", name = "lucene-queryparser"),
//    ExclusionRule(organization = "org.apache.lucene", name = "lucene-sandbox"),
//    ExclusionRule(organization = "org.apache.lucene", name = "lucene-spatial"),
//    ExclusionRule(organization = "org.apache.lucene", name = "lucene-suggest")
//    )
  "jp.co.bizreach" %% "elastic-scala-httpclient" % "1.0.5",
  "jp.t2v" %% "play2-auth"        % "0.14.0",
  play.sbt.Play.autoImport.cache
)

libraryDependencies += "com.ning" % "async-http-client" % "1.9.29" force()
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m"
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

