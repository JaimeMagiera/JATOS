import com.typesafe.config._

name := "jatos-common"

version := "2.1.1-beta"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
	javaCore,
	javaJdbc,
	javaJpa,
	javaWs,
	evolutions,
	"org.hibernate" % "hibernate-entitymanager" % "4.3.11.Final",
	"mysql" % "mysql-connector-java" % "5.1.31",
	"org.jsoup" % "jsoup" % "1.8.1",
	"commons-io" % "commons-io" % "2.4",
	"org.mockito" % "mockito-core" % "1.9.5" % "test"
)