package pl.msitko.realworld.entities

import pl.msitko.realworld.db
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
object UserBody:
  def fromDB(dbUser: db.User, jwtToken: String): UserBody =
    UserBody(user = User(
      email = dbUser.email,
      token = jwtToken,
      username = dbUser.username,
      bio = dbUser.bio,
      image = dbUser.image,
    ))

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

final case class UpdateUserReqBody(user: UpdateUserBody):
  def toDB(existingUser: db.User): db.UpdateUser =
    db.UpdateUser(
      email = user.email.getOrElse(existingUser.email),
      username = user.username.getOrElse(existingUser.username),
      password = user.password,
      // TODO: Current treatment of bio and image is problematic in case of nullifying those values as part of update
      // On the other hand specs don't tell anything explicitly about nullifying. I guess something like following
      // would make sense:
      // Omitting value in update request means "no change"
      // Specifying value to be null explicitly means "change the value to null"
      bio = user.bio.orElse(existingUser.bio),
      image = user.image.orElse(existingUser.image)
    )
