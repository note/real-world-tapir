package pl.msitko.realworld.db

import doobie.*
import doobie.implicits.*
import doobie.implicits.legacy.instant.*
import java.time.Instant

final case class CommentNoId(
    authorId: UserId,
    articleId: ArticleId,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
)

final case class Comment(
    id: Int,
    authorId: UserId,
    articleId: ArticleId,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
)

final case class FullComment(
    comment: Comment,
    author: Author
)

object FullComment:
  implicit val fullCommentRead: Read[FullComment] =
    Read[(Int, UserId, ArticleId, String, Instant, Instant, String, Option[String], Option[String], Option[Int])].map {
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
