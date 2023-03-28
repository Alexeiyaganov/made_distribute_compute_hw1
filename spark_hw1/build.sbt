name := "spark_hw1"

version := "0.1"

scalaVersion := "2.12.10"

val sparkVersion = "3.0.1"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql" % sparkVersion withSources(),
  "org.apache.spark" %% "spark-mllib" % sparkVersion withSources()
)

libraryDependencies += ("org.scalatest" %% "scalatest" % "3.2.2" % "test" withSources())

