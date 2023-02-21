package pl.msitko.realworld

import doobie.Meta

import java.util.UUID
import doobie.postgres.implicits.*

package object db:

  private type ArticleIdTag
  type ArticleId = ArticleIdTag & UUID
  def liftToArticleId(uuid: UUID): ArticleId =
    uuid.asInstanceOf[ArticleId]
  implicit val articleIdMeta: Meta[ArticleId] =
    Meta[UUID].imap(liftToArticleId)(identity)

//  private type AuthenticatedUserIdTag
//  type AuthenticatedUserId = AuthenticatedUserIdTag & UUID
//  def liftToAuthenticatedUserId(uuid: UUID): AuthenticatedUserId =
//    uuid.asInstanceOf[AuthenticatedUserId]
//  implicit val authenticatedUserIduserIdMeta: Meta[AuthenticatedUserId] =
//    Meta[UUID].imap(liftToAuthenticatedUserId)(identity)

  private type UserIdTag
  type UserId = UserIdTag & UUID
  def liftToUserId(uuid: UUID): UserId =
    uuid.asInstanceOf[UserId]
  implicit val userIdMeta: Meta[UserId] =
    Meta[UUID].imap(liftToUserId)(identity)

  private type TagIdTag
  type TagId = TagIdTag & UUID
  def liftToTagId(uuid: UUID): TagId =
    uuid.asInstanceOf[TagId]
  implicit val tagIdMeta: Meta[TagId] =
    Meta[UUID].imap(liftToTagId)(identity)
