organization := "com.github.skozlov"
name := "async"
version := "0.1.0"
isSnapshot := true
scalaVersion := "2.13.8"

scalacOptions ++= Seq(
    "-encoding", "utf8",
//    "-Xfatal-warnings", todo
    "-Xlint",
    "-target:jvm-17",
    "-feature", "-language:implicitConversions",
)
