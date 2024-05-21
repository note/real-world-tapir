package pl.msitko.realworld.entities

import cats.implicits.*
import pl.msitko.realworld.*

final case class AuthenticationReqBodyUser(email: String, password: String)
final case class AuthenticationReqBody(user: AuthenticationReqBodyUser)
final case class RegistrationReqBody(user: RegistrationUserBody)

final case class User(
    email: String,
    token: String,
    username: String,
    bio: Option[String],
    image: Option[String],
):
  def body: UserBody = UserBody(user = this)

final case class UserBody(user: User)

final case class RegistrationUserBody(
    username: String,
    email: String,
    password: String,
    bio: Option[String],
    image: Option[String]):
  override def toString: String =
    s"RegistrationUserBody($username, $email, <masked>, $bio, $image)"

final case class UpdateUserBody(
    email: Option[String],
    username: Option[String],
    password: Option[String],
    image: Option[String],
    bio: Option[String],
)

final case class UpdateUserReqBody(user: UpdateUserBody)
