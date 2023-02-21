package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.Entities
import pl.msitko.realworld.db.TagRepo

object TagService:
  def apply(repos: Repos): TagService =
    new TagService(repos.tagRepo)

class TagService(tagRepo: TagRepo):
  def getTags: IO[Entities.Tags] =
    tagRepo.getAllTags.map(Entities.Tags.fromDB)
