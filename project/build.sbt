scalacOptions += "-deprecation"

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.1")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.0.4")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

//someProject.enablePlugins(SbtTwirl)
//lazy val root = (project in file(".")).enablePlugins(SbtTwirl)
//TwirlKeys.templateImports += "org.example._"
//sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value
//addSbtPlugin("com.lihaoyi" %% "scalatex" % "0.1.0")
