package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.implicits.legacy.instant.*

import java.time.Instant
import java.util.UUID

final case class ArticleNoId(
    slug: String,
    title: String,
    description: String,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
)

final case class Article(
    id: UUID,
    slug: String,
    title: String,
    description: String,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
    authorId: UUID,
)

class ArticleRepo(transactor: Transactor[IO]):

  def insert(article: ArticleNoId, authorId: UUID): IO[Article] =
    sql"""INSERT INTO public.articles (author_id, slug, title, description, body, created_at, updated_at) 
         |VALUES ($authorId, ${article.slug}, ${article.title}, ${article.description}, ${article.body}, ${article.createdAt}, ${article.updatedAt})""".stripMargin.update
      .withUniqueGeneratedKeys[Article](
        "id",
        "slug",
        "title",
        "description",
        "body",
        "created_at",
        "updated_at",
        "author_id")
      .transact(transactor)

  def getArticle(slug: String): IO[Option[Article]] =
    sql"SELECT id, author_id, slug, title, description, body, created_at, updated_at FROM public.articles WHERE slug=$slug"
      .query[Article]
      .option
      .transact(transactor)
