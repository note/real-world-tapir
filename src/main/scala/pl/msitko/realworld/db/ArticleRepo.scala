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

final case class UpdateArticle(
    slug: String,
    title: String,
    description: String,
    body: String,
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

  def update(ch: UpdateArticle, articleId: UUID): IO[Int] =
    sql"""UPDATE public.articles SET 
         |slug = ${ch.slug},
         |title = ${ch.title},
         |description = ${ch.description},
         |body = ${ch.body},
         |updated_at = NOW()
         |WHERE id=$articleId
       """.stripMargin.update.run.transact(transactor)

  def getBySlug(slug: String): IO[Option[Article]] =
    sql"SELECT id, slug, title, description, body, created_at, updated_at, author_id FROM public.articles WHERE slug=$slug"
      .query[Article]
      .option
      .transact(transactor)

  def getById(id: UUID): IO[Option[Article]] =
    sql"SELECT id, slug, title, description, body, created_at, updated_at, author_id FROM public.articles WHERE id=$id"
      .query[Article]
      .option
      .transact(transactor)

  def delete(slug: String): IO[Int] =
    sql"DELETE FROM public.articles WHERE slug=$slug".update.run.transact(transactor)
