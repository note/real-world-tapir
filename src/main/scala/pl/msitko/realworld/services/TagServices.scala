package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.Entities
import pl.msitko.realworld.endpoints.TagEndpoints

object TagServices:
  val getTagsImpl =
    TagEndpoints.getTags.serverLogicSuccess(_ => IO.pure(Entities.Tags(tags = List("a", "b"))))

  val services = List(getTagsImpl)
