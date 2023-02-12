package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.implicits.legacy.instant.*

import java.time.Instant
import java.util.UUID
import javax.swing.plaf.SliderUI

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

// FullArticle represents a result row of data related to a single article and JOINed over many tables
final case class FullArticle(
    article: Article,
    author: ArticleAuthor,
    favoritesCount: Option[Int],
    favorited: Option[Int],
    tags: List[String]
)

final case class ArticleAuthor(
    username: String,
    bio: Option[String],
)

final case class UpdateArticle(
    slug: String,
    title: String,
    description: String,
    body: String,
)

class ArticleRepo(transactor: Transactor[IO]):

  implicit val fullArticleRead: Read[FullArticle] =
    Read[(
        UUID,
        String,
        String,
        String,
        String,
        Instant,
        Instant,
        UUID,
        String,
        Option[String],
        Option[Int],
        Option[Int],
        Option[String])]
      .map {
        case (
              id,
              slug,
              title,
              description,
              body,
              createdAt,
              updatedAt,
              authorId,
              authorUsername,
              authorBio,
              favoritesCount,
              favorited,
              tags) =>
          val article = Article(
            id = id,
            slug = slug,
            title = title,
            description = description,
            body = body,
            createdAt = createdAt,
            updatedAt = updatedAt,
            authorId = authorId)
          FullArticle(
            article = article,
            author = ArticleAuthor(
              username = authorUsername,
              bio = authorBio
            ),
            favoritesCount = favoritesCount,
            favorited = favorited,
            // TODO: document and enforce no commas in tag names
            tags = tags.map(_.split(',').toList).getOrElse(List.empty)
          )
      }

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

  def getBySlug(slug: String, subjectUserId: UUID): IO[Option[FullArticle]] =
    (for {
      articleIdOpt <- idForSlug(slug)
      article <- articleIdOpt match
        case Some(articleId) =>
          getById2(articleId, subjectUserId)
        case None =>
          doobie.free.connection.pure(None)
    } yield article).transact(transactor)

  def getBySlug(slug: String): IO[Option[FullArticle]] =
    (for {
      articleIdOpt <- idForSlug(slug)
      article <- articleIdOpt match
        case Some(articleId) =>
          getById3(articleId)
        case None =>
          doobie.free.connection.pure(None)
    } yield article).transact(transactor)

  def getById(id: UUID, subjectUserId: UUID): IO[Option[FullArticle]] =
    getById2(id, subjectUserId).transact(transactor)

  def getById(id: UUID): IO[Option[FullArticle]] =
    getById3(id).transact(transactor)

  def delete(slug: String): IO[Int] =
    sql"DELETE FROM public.articles WHERE slug=$slug".update.run.transact(transactor)

  def insertFavorite(articleId: UUID, userId: UUID): IO[Int] =
    sql"INSERT INTO public.favorites (article_id, user_id) VALUES ($articleId, $userId)".update.run.transact(transactor)

  private def idForSlug(slug: String): doobie.ConnectionIO[Option[UUID]] =
    sql"SELECT id FROM articles WHERE slug=$slug"
      .query[UUID]
      .option

  // TODO: rename
  private def getById2(articleId: UUID, subjectUserId: UUID): doobie.ConnectionIO[Option[FullArticle]] =
    sql"""
         |WITH favoritez AS (SELECT article_id, COUNT(user_id) count FROM favorites WHERE article_id=$articleId GROUP BY article_id),
         |     tagz      AS (SELECT a.id article_id, STRING_AGG(distinct t2.tag, ',' ORDER BY t2.tag ASC) tags from articles a JOIN articles_tags t on a.id = t.article_id JOIN tags t2 on t.tag_id = t2.id WHERE article_id=$articleId GROUP BY a.id)
         |SELECT a.id, a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id, u.username, u.bio, f.count as favoritez_count,
         |       (select count(user_id) from favorites where article_id=a.id and user_id=${subjectUserId} group by user_id) favorited,
         |       t.tags
         |FROM articles a
         |JOIN users u ON a.author_id = u.id
         |LEFT JOIN favoritez f ON a.id = f.article_id
         |LEFT JOIN tagz t ON a.id = t.article_id
         |WHERE a.id=$articleId;
       """.stripMargin
      .query[FullArticle]
      .option

  // TODO: rename
  private def getById3(articleId: UUID): doobie.ConnectionIO[Option[FullArticle]] =
    sql"""
         |WITH favoritez AS (SELECT article_id, COUNT(user_id) count FROM favorites WHERE article_id=$articleId GROUP BY article_id),
         |     tagz      AS (SELECT a.id article_id, STRING_AGG(distinct t2.tag, ',' ORDER BY t2.tag ASC) tags from articles a JOIN articles_tags t on a.id = t.article_id JOIN tags t2 on t.tag_id = t2.id WHERE article_id=$articleId GROUP BY a.id)
         |SELECT a.id, a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id, u.username, u.bio, f.count as favoritez_count,
         |       NULL,
         |       t.tags
         |FROM articles a
         |JOIN users u ON a.author_id = u.id
         |LEFT JOIN favoritez f ON a.id = f.article_id
         |LEFT JOIN tagz t ON a.id = t.article_id
         |WHERE a.id=$articleId;
       """.stripMargin
      .query[FullArticle]
      .option

// TODO: remove
//  def countFavorites(articleId: UUID): IO[Int] =
//    sql"SELECT COUNT(user_id) from public.favorites where article_id=$articleId".query[Int].unique.transact(transactor)
//
//  def favoritedBy(articleId: UUID, userId: UUID): IO[Boolean] =
//    sql"SELECT COUNT(user_id) > 0 from public.favorites where article_id=$articleId AND user_id=$userId"
//      .query[Boolean]
//      .unique
//      .transact(transactor)
