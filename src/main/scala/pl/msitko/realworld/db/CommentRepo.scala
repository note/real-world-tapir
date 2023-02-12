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
    Read[(Int, UUID, UUID, String, Instant, Instant, String, Option[String])].map {
      case (id, authorId, articleId, body, createdAt, updatedAt, username, bio) =>
        val comment = Comment(
          id = id,
          authorId = authorId,
          articleId = articleId,
          body = body,
          createdAt = createdAt,
          updatedAt = updatedAt)
        val author = Author(username = username, bio = bio)
        FullComment(comment, author)
    }

  def insert(comment: CommentNoId): IO[Comment] =
    sql"""INSERT INTO comments (author_id, article_id, body, created_at, updated_at)
         |VALUES (${comment.authorId}, ${comment.articleId}, ${comment.body}, ${comment.createdAt}, ${comment.updatedAt})
       """.stripMargin.update
      .withUniqueGeneratedKeys[Comment]("id", "author_id", "article_id", "body", "created_at", "updated_at")
      .transact(transactor)

  def getForCommentId(commentId: Int): IO[Option[FullComment]] =
    sql"""SELECT c.id, c.author_id, c.article_id, c.body, c.created_at, c.updated_at, u.username, u.bio
         |    FROM comments c 
         |    JOIN users u ON c.author_id = u.id 
         |    WHERE c.id=$commentId""".stripMargin.query[FullComment].option.transact(transactor)

  def getForArticleId(articleId: UUID): IO[List[FullComment]] =
    sql"""SELECT c.id, c.author_id, c.article_id, c.body, c.created_at, c.updated_at, u.username, u.bio
         |    FROM comments c 
         |    JOIN users u ON c.author_id = u.id 
         |    WHERE c.article_id=$articleId""".stripMargin.query[FullComment].to[List].transact(transactor)

  def delete(commentId: Int): IO[Int] =
    sql"DELETE FROM comments WHERE id=$commentId".update.run.transact(transactor)
