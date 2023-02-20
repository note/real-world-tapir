package pl.msitko.realworld.db

import cats.effect.IO
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.implicits.legacy.instant.*

class CommentRepo(transactor: Transactor[IO]):
  def insert(comment: CommentNoId): IO[Comment] =
    sql"""INSERT INTO comments (author_id, article_id, body, created_at, updated_at)
         |VALUES (${comment.authorId}, ${comment.articleId}, ${comment.body}, ${comment.createdAt}, ${comment.updatedAt})
       """.stripMargin.update
      .withUniqueGeneratedKeys[Comment]("id", "author_id", "article_id", "body", "created_at", "updated_at")
      .transact(transactor)

  def getForCommentId(commentId: Int, subjectUserId: UserId): IO[Option[FullComment]] =
    sql"""WITH followerz AS (SELECT followed, COUNT(follower) count FROM followers WHERE follower=$subjectUserId GROUP BY followed)
         |                SELECT c.id, c.author_id, c.article_id, c.body, c.created_at, c.updated_at, u.username, u.bio, u.image, f.count
         |                FROM comments c
         |                JOIN users u ON c.author_id = u.id
         |                LEFT JOIN followerz f ON c.author_id = f.followed
         |                WHERE c.id=$commentId;""".stripMargin.query[FullComment].option.transact(transactor)

  def getForArticleId(articleId: ArticleId, subjectUserId: Option[UserId]): IO[List[FullComment]] =
    subjectUserId match
      case Some(uid) => getForArticleId(articleId, uid)
      case None      => getForArticleId(articleId)

  def getForArticleId(articleId: ArticleId): IO[List[FullComment]] =
    sql"""SELECT c.id, c.author_id, c.article_id, c.body, c.created_at, c.updated_at, u.username, u.bio, u.image, NULL
         |    FROM comments c 
         |    JOIN users u ON c.author_id = u.id 
         |    WHERE c.article_id=$articleId""".stripMargin.query[FullComment].to[List].transact(transactor)

  def getForArticleId(articleId: ArticleId, subjectUserId: UserId): IO[List[FullComment]] =
    sql"""WITH followerz AS (SELECT followed, COUNT(follower) count FROM followers WHERE follower=$subjectUserId GROUP BY followed)
         |SELECT c.id, c.author_id, c.article_id, c.body, c.created_at, c.updated_at, u.username, u.bio, u.image, f.count
         |FROM comments c
         |         JOIN users u ON c.author_id = u.id
         |         LEFT JOIN followerz f ON c.author_id = f.followed
         |WHERE c.article_id=$articleId""".stripMargin.query[FullComment].to[List].transact(transactor)

  def delete(commentId: Int): IO[Int] =
    sql"DELETE FROM comments WHERE id=$commentId".update.run.transact(transactor)
