package pl.msitko.realworld.entities

import pl.msitko.realworld.db
import pl.msitko.realworld.db.{ArticleId, UserId}

import java.time.Instant

final case class AddCommentReq(body: String)

final case class AddCommentReqBody(comment: AddCommentReq):
  def toDB(authorId: UserId, articleId: ArticleId, now: Instant): db.CommentNoId =
    db.CommentNoId(authorId = authorId, articleId = articleId, body = comment.body, createdAt = now, updatedAt = now)

final case class Comments(comments: List[Comment])

final case class Comment(id: Int, createdAt: Instant, updatedAt: Instant, body: String, author: Profile):
  def toBody: CommentBody = CommentBody(comment = this)
object Comment:
  def fromDB(dbComment: db.FullComment): Comment =
    Comment(
      id = dbComment.comment.id,
      createdAt = dbComment.comment.createdAt,
      updatedAt = dbComment.comment.createdAt,
      body = dbComment.comment.body,
      author = Profile.fromDB(dbComment.author))

final case class CommentBody(comment: Comment)
object CommentBody:
  def fromDB(dbComment: db.FullComment): CommentBody =
    CommentBody(comment = Comment.fromDB(dbComment))
