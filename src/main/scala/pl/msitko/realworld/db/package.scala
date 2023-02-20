package pl.msitko.realworld

import doobie.Meta

import java.util.UUID
import doobie.postgres.implicits.*

package object db:

  type ArticleIdTag
  type ArticleId = ArticleIdTag & UUID
  def liftToArticleId(uuid: UUID): ArticleId =
    uuid.asInstanceOf[ArticleId]
  implicit val articleIdMeta: Meta[ArticleId] =
    Meta[UUID].imap(liftToArticleId)(identity)

  type UserIdTag
  type UserId = UserIdTag & UUID
  def liftToUserId(uuid: UUID): UserId =
    uuid.asInstanceOf[UserId]
  implicit val userIdMeta: Meta[UserId] =
    Meta[UUID].imap(liftToUserId)(identity)

  type TagId = UUID

//  def creatingInstances(): Int =
//    ArticleId
//    55

  def a(articleId: ArticleId): Int =
    articleId.version
//    u(articleId)

  def u(userId: UserId): Int = ???
