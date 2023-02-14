package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.Entities

object TagServices:
  def getTags: IO[Entities.Tags] =
    IO.pure(Entities.Tags(tags = List("a", "b")))
