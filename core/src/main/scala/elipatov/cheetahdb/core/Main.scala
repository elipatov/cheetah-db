package elipatov.cheetahdb.core

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    IO.delay(println("Hello, World!"))
      .as(ExitCode.Success)
  }
}