package pl.msitko.realworld.entities

import java.time.Instant

final case class AddCommentReq(body: String)

final case class AddCommentReqBody(comment: AddCommentReq)

final case class Comments(comments: List[Comment])

final case class Comment(id: Int, createdAt: Instant, updatedAt: Instant, body: String, author: Profile):
  def toBody: CommentBody = CommentBody(comment = this)

final case class CommentBody(comment: Comment)
