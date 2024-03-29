package pl.msitko.realworld.db

import cats.data.NonEmptyList
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import doobie.*
import doobie.implicits.*
import doobie.implicits.legacy.instant.*
import doobie.postgres.sqlstate
import pl.msitko.realworld.endpoints.ErrorInfo

import scala.util.Random

class ArticleRepo(transactor: Transactor[IO]) extends StrictLogging:
  private def justInsert(article: ArticleNoId, authorId: UserId): IO[Either[ErrorInfo.ValidationError, Article]] =
    sql"""INSERT INTO articles (author_id, slug, title, description, body, created_at, updated_at)
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
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        ErrorInfo.ValidationError.fromTuple("article.slug" -> "An article with a given slug already exists")
      }
      .transact(transactor)

  private def generateSlug(slug: String): String = s"$slug-${Random.alphanumeric.take(7).mkString}"

  def insert(article: ArticleNoId, authorId: UserId): IO[Either[ErrorInfo, Article]] =
    justInsert(article, authorId)
      .flatMap {
        case Left(err) =>
          logger.info(s"Couldn't insert an article because of slug duplicate: $err for ${article.slug}")
          val articleCopy = article.copy(slug = generateSlug(article.slug))
          justInsert(articleCopy, authorId)
        case Right(value) =>
          IO.pure(Right(value))
      }

  private def justUpdate(ch: UpdateArticle, articleId: ArticleId): IO[Either[ErrorInfo.ValidationError, Int]] =
    sql"""UPDATE articles SET
         |slug = ${ch.slug},
         |title = ${ch.title},
         |description = ${ch.description},
         |body = ${ch.body},
         |updated_at = NOW()
         |WHERE id=$articleId
       """.stripMargin.update.run
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        ErrorInfo.ValidationError.fromTuple("article.slug" -> "An article with a given slug already exists")
      }
      .transact(transactor)

  def update(ch: UpdateArticle, articleId: ArticleId): IO[Either[ErrorInfo.ValidationError, Int]] =
    justUpdate(ch, articleId)
      .flatMap {
        case Left(err) =>
          logger.info(s"Couldn't update an article because of slug duplicate: $err for $ch")
          val updatedChange = ch.copy(slug = generateSlug(ch.slug))
          justUpdate(updatedChange, articleId)
        case Right(value) =>
          IO.pure(Right(value))
      }

  def getBySlug(slug: String, subjectUserId: UserId): IO[Option[FullArticle]] =
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

  def getById(id: ArticleId, subjectUserId: UserId): IO[Option[FullArticle]] =
    getById2(id, subjectUserId).transact(transactor)

  def delete(slug: String): IO[Int] =
    sql"DELETE FROM articles WHERE slug=$slug".update.run.transact(transactor)

  def insertFavorite(articleId: ArticleId, userId: UserId): IO[Int] =
    sql"INSERT INTO favorites (article_id, user_id) VALUES ($articleId, $userId)".update.run
      .exceptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        doobie.free.connection.pure(0)
      }
      .transact(transactor)

  def deleteFavorite(articleId: ArticleId, userId: UserId): IO[Int] =
    sql"DELETE FROM favorites WHERE article_id=$articleId AND user_id=$userId".update.run.transact(transactor)

  // Can we somehow extract common parts of SQLs (e.g. CTEs)?
  def feed(subjectUserId: UserId, followed: NonEmptyList[UserId], pagination: Pagination): IO[List[FullArticle]] =
    val q = fr"""
        WITH favoritez AS (SELECT article_id, COUNT(user_id) count FROM favorites GROUP BY article_id),
             tagz      AS (SELECT a.id article_id, STRING_AGG(distinct t2.tag, ',' ORDER BY t2.tag ASC) tags from articles a JOIN articles_tags t on a.id = t.article_id JOIN tags t2 on t.tag_id = t2.id GROUP BY a.id),
             followerz AS (SELECT followed, COUNT(follower) count FROM followers WHERE follower=$subjectUserId GROUP BY followed)
        SELECT a.id, a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id, u.username, u.bio, u.image, f.count as favoritez_count,
               (select count(user_id) from favorites where article_id=a.id and user_id=${subjectUserId} group by user_id) favorited,
               flrz.count,
               t.tags
        FROM articles a
        JOIN users u ON a.author_id = u.id
        LEFT JOIN favoritez f ON a.id = f.article_id
        LEFT JOIN tagz t ON a.id = t.article_id
        LEFT JOIN followerz flrz ON a.author_id = flrz.followed
        WHERE """ ++ Fragments.in(
      fr"a.author_id",
      followed) ++ fr" ORDER BY a.created_at DESC LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    q.query[FullArticle]
      .to[List]
      .transact(transactor)

  def listArticles(
      query: ArticleQuery[UserCoordinates],
      pagination: Pagination,
      subjectUserId: Option[UserId]): IO[List[FullArticle]] =
    subjectUserId match
      case Some(uid) => listArticles(query, pagination, uid)
      case None      => listArticles(query, pagination)

  def listArticles(
      query: ArticleQuery[UserCoordinates],
      pagination: Pagination,
      subjectUserId: UserId): IO[List[FullArticle]] =
    val favoritedPart = query.favoritedBy match
      case Some(u) =>
        fr" JOIN favorites ON (a.id=favorites.article_id AND favorites.user_id=${u.id}) "
      case None =>
        fr" "

    val tagPart = query.tag match
      case Some(tag) =>
        fr" JOIN articles_tags att ON a.id=att.article_id JOIN tags tt ON (att.tag_id=tt.id AND tt.tag=$tag) "
      case None =>
        fr" "

    val authorPart = query.author match
      case Some(requestedAuthor) =>
        fr" WHERE u.username = $requestedAuthor "
      case None =>
        fr" "

    val q =
      fr"""WITH favoritez AS (SELECT article_id, COUNT(user_id) count FROM favorites GROUP BY article_id),
          |             tagz      AS (SELECT a.id article_id, STRING_AGG(distinct t2.tag, ',' ORDER BY t2.tag ASC) tags from articles a JOIN articles_tags t on a.id = t.article_id JOIN tags t2 on t.tag_id = t2.id GROUP BY a.id),
          |             followerz AS (SELECT followed, COUNT(follower) count FROM followers WHERE follower=$subjectUserId GROUP BY followed)
          |        SELECT a.id, a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id, u.username, u.bio, u.image, f.count as favoritez_count,
          |               (select count(user_id) from favorites where article_id=a.id and user_id=${subjectUserId} group by user_id) favorited,
          |               flrz.count,
          |               t.tags
          |        FROM articles a
          |        $favoritedPart
          |        $tagPart
          |        JOIN users u ON a.author_id = u.id
          |        LEFT JOIN favoritez f ON a.id = f.article_id
          |        LEFT JOIN tagz t ON a.id = t.article_id
          |        LEFT JOIN followerz flrz ON a.author_id = flrz.followed $authorPart ORDER BY a.created_at DESC LIMIT ${pagination.limit} OFFSET ${pagination.offset}""".stripMargin

    q.query[FullArticle].to[List].transact(transactor)

  def listArticles(query: ArticleQuery[UserCoordinates], pagination: Pagination): IO[List[FullArticle]] =
    val favoritedPart = query.favoritedBy match
      case Some(u) =>
        fr" JOIN favorites ON (a.id=favorites.article_id AND favorites.user_id=${u.id}) "
      case None =>
        fr" "

    val tagPart = query.tag match
      case Some(tag) =>
        fr" JOIN articles_tags att ON a.id=att.article_id JOIN tags tt ON (att.tag_id=tt.id AND tt.tag=$tag) "
      case None =>
        fr" "

    val authorPart = query.author match
      case Some(requestedAuthor) =>
        fr" WHERE u.username = $requestedAuthor "
      case None =>
        fr" "

    val q =
      fr"""WITH favoritez AS (SELECT article_id, COUNT(user_id) count FROM favorites GROUP BY article_id),
          |             tagz      AS (SELECT a.id article_id, STRING_AGG(distinct t2.tag, ',' ORDER BY t2.tag ASC) tags from articles a JOIN articles_tags t on a.id = t.article_id JOIN tags t2 on t.tag_id = t2.id GROUP BY a.id)
          |        SELECT a.id, a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id, u.username, u.bio, u.image, f.count as favoritez_count,
          |               NULL,
          |               NULL,
          |               t.tags
          |        FROM articles a
          |        $favoritedPart
          |        $tagPart
          |        JOIN users u ON a.author_id = u.id
          |        LEFT JOIN favoritez f ON a.id = f.article_id
          |        LEFT JOIN tagz t ON a.id = t.article_id $authorPart ORDER BY a.created_at DESC LIMIT ${pagination.limit} OFFSET ${pagination.offset}""".stripMargin

    q.query[FullArticle].to[List].transact(transactor)

  private def idForSlug(slug: String): doobie.ConnectionIO[Option[ArticleId]] =
    sql"SELECT id FROM articles WHERE slug=$slug"
      .query[ArticleId]
      .option

  // TODO: rename
  private def getById2(articleId: ArticleId, subjectUserId: UserId): doobie.ConnectionIO[Option[FullArticle]] =
    sql"""
         |WITH favoritez AS (SELECT article_id, COUNT(user_id) count FROM favorites WHERE article_id=$articleId GROUP BY article_id),
         |     tagz      AS (SELECT a.id article_id, STRING_AGG(distinct t2.tag, ',' ORDER BY t2.tag ASC) tags from articles a JOIN articles_tags t on a.id = t.article_id JOIN tags t2 on t.tag_id = t2.id WHERE article_id=$articleId GROUP BY a.id),
         |     followerz AS (SELECT followed, COUNT(follower) count FROM followers WHERE follower=$subjectUserId GROUP BY followed)
         |SELECT a.id, a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id, u.username, u.bio, u.image, f.count as favoritez_count,
         |       (select count(user_id) from favorites where article_id=a.id and user_id=${subjectUserId} group by user_id) favorited,
         |       flrz.count,
         |       t.tags
         |FROM articles a
         |JOIN users u ON a.author_id = u.id
         |LEFT JOIN favoritez f ON a.id = f.article_id
         |LEFT JOIN tagz t ON a.id = t.article_id
         |LEFT JOIN followerz flrz ON a.author_id = flrz.followed
         |WHERE a.id=$articleId;
     """.stripMargin
      .query[FullArticle]
      .option

  // TODO: rename
  private def getById3(articleId: ArticleId): doobie.ConnectionIO[Option[FullArticle]] =
    sql"""
         |WITH favoritez AS (SELECT article_id, COUNT(user_id) count FROM favorites WHERE article_id=$articleId GROUP BY article_id),
         |     tagz      AS (SELECT a.id article_id, STRING_AGG(distinct t2.tag, ',' ORDER BY t2.tag ASC) tags from articles a JOIN articles_tags t on a.id = t.article_id JOIN tags t2 on t.tag_id = t2.id WHERE article_id=$articleId GROUP BY a.id)
         |SELECT a.id, a.slug, a.title, a.description, a.body, a.created_at, a.updated_at, a.author_id, u.username, u.bio, u.image, f.count as favoritez_count,
         |       NULL,
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
