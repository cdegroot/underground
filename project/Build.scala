import sbt._
import Keys._

object HelloBuild extends Build {
    lazy val root = Project(id = "underground", 
                            base = file(".")) aggregate(performance,main)

    lazy val main = Project(id = "underground-main",
                            base = file("main")) 

    lazy val performance = Project(id = "underground-perf",
                                   base = file("perf")) dependsOn(main)
}
