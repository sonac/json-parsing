import cats.effect._
import cats.syntax.all._
import scopt.OParser

import ArgConfig.parser
import Service._

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    if (args.nonEmpty) {
      OParser.parse(parser, args, ArgConfig()) match {
        case Some(args) =>
          IO {
            val parsedData = parseFiles(args.files.toList)
            val processedData = processData(parsedData)
            writeResults(
              processedData._1,
              processedData._2,
              System.getProperty("user.dir")
            )
          }.as(ExitCode.Success)
        case None => IO(System.err.println("Wrong arguments")).as(ExitCode(2))
      }
    } else IO(System.err.println("Provide arguments")).as(ExitCode(2))
  }

}
