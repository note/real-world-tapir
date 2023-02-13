package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.implicits.legacy.instant.*

import java.util.UUID
import java.time.Instant

final case class CommentNoId(
    authorId: UUID,
    articleId: UUID,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
)

final case class Comment(
    id: Int,
    authorId: UUID,
    articleId: UUID,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
)

final case class FullComment(
    comment: Comment,
    author: Author
)

class CommentRepo(transactor: Transactor[IO]):
  implicit val fullCommentRead: Read[FullComment] =
    Read[(Int, UUID, UUID, String, Instant, Instant, String, Option[String], Option[String], Option[Int])].map {
      case (id, authorId, articleId, body, createdAt, updatedAt, username, bio, authorImage, following) =>
        val comment = Comment(
          id = id,
          authorId = authorId,
          articleId = articleId,
          body = body,
          createdAt = createdAt,
          updatedAt = updatedAt)
        val author = Author(username = username, bio = bio, image = authorImage, following = following.isDefined)
        FullComment(comment, author)
    }

  def insert(comment: CommentNoId): IO[Comment] =
    sql"""INSERT INTO comments (author_id, article_id, body, created_at, updated_at)
         |VALUES (${comment.authorId}, ${comment.articleId}, ${comment.body}, ${comment.createdAt}, ${comment.updatedAt})
       """.stripMargin.update
      .withUniqueGeneratedKeys[Comment]("id", "author_id", "article_id", "body", "created_at", "updated_at")
      .transact(transactor)

  def getForCommentId(commentId: Int, subjectUserId: UUID): IO[Option[FullComment]] =
    sql"""WITH followerz AS (SELECT followed, COUNT(follower) count FROM followers WHERE follower=$subjectUserId GROUP BY followed)
         |                SELECT c.id, c.author_id, c.article_id, c.body, c.created_at, c.updated_at, u.username, u.bio, u.image, f.count
         |                FROM comments c
         |                JOIN users u ON c.author_id = u.id
         |                LEFT JOIN followerz f ON c.author_id = f.followed
         |                WHERE c.id=$commentId;""".stripMargin.query[FullComment].option.transact(transactor)

  def getForArticleId(articleId: UUID, subjectUserId: Option[UUID]): IO[List[FullComment]] =
    subjectUserId match
      case Some(uid) => getForArticleId(articleId, uid)
      case None      => getForArticleId(articleId)

  def getForArticleId(articleId: UUID): IO[List[FullComment]] =
    sql"""SELECT c.id, c.author_id, c.article_id, c.body, c.created_at, c.updated_at, u.username, u.bio, u.image, NULL
         |    FROM comments c 
         |    JOIN users u ON c.author_id = u.id 
         |    WHERE c.article_id=$articleId""".stripMargin.query[FullComment].to[List].transact(transactor)

  def getForArticleId(articleId: UUID, subjectUserId: UUID): IO[List[FullComment]] =
    sql"""WITH followerz AS (SELECT followed, COUNT(follower) count FROM followers WHERE follower=$subjectUserId GROUP BY followed)
         |SELECT c.id, c.author_id, c.article_id, c.body, c.created_at, c.updated_at, u.username, u.bio, u.image, f.count
         |FROM comments c
         |         JOIN users u ON c.author_id = u.id
         |         LEFT JOIN followerz f ON c.author_id = f.followed
         |WHERE c.article_id=$articleId""".stripMargin.query[FullComment].to[List].transact(transactor)

  def delete(commentId: Int): IO[Int] =
    sql"DELETE FROM comments WHERE id=$commentId".update.run.transact(transactor)
