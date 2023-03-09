package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.db.TagRepo
import pl.msitko.realworld.entities.OtherEntities

object TagService:
  def apply(repos: Repos): TagService =
    new TagService(repos.tagRepo)

class TagService(tagRepo: TagRepo):
  def getTags: IO[OtherEntities.Tags] =
    tagRepo.getAllTags.map(OtherEntities.Tags.fromDB)
