import scopt.{OParser, OParserBuilder}

case class ArgConfig(
    files: Seq[String] = Seq()
)

object ArgConfig {

  val builder: OParserBuilder[ArgConfig] = OParser.builder[ArgConfig]
  val parser = {
    import builder._
    OParser.sequence(
      programName("scopt"),
      head("scopt", "4.0"),
      opt[Seq[String]]('u', "files")
        .valueName("<file1>,<file2>...")
        .action((x, c) => c.copy(files = x))
        .text("files to process"),
      help("help").text(
        "Usage : sbt run " +
          "--files <files to process>"
      )
    )
  }

}
