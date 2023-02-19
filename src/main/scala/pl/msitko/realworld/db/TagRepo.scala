package pl.msitko.realworld.db

import cats.data.NonEmptyList
import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.implicits.legacy.instant.*

import java.util.UUID

final case class ArticleTag(
    articleId: UUID,
    tagId: UUID,
)

class TagRepo(transactor: Transactor[IO]):
  def upsertTags(tags: NonEmptyList[String]): IO[List[UUID]] =
    val q = fr"INSERT INTO tags (tag) " ++ Fragments.values(tags)
    q.update
      .withGeneratedKeys[UUID](
        "id"
      )
      .take(tags.size)
      .compile
      .toList
      .transact(transactor)

  // TODO: test if PUTing different set of tags actually works
  def insertArticleTags(articleTags: NonEmptyList[ArticleTag]): IO[Int] =
    val q = fr"INSERT INTO articles_tags (article_id, tag_id) " ++ Fragments.values(articleTags)
    q.update.run
    q.update.run.transact(transactor)
