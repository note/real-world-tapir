package pl.msitko.realworld.entities

import pl.msitko.realworld.db
import pl.msitko.realworld.*
import cats.implicits.*

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
  def toDB(existingUser: db.User): Validated[db.UpdateUser] =
    (
      user.email
        .map(newEmail => Validation.validEmail("user.email")(newEmail))
        .getOrElse(existingUser.email.validNec),
      user.username
        .map(newUserName => Validation.nonEmptyString("user.username")(newUserName))
        .getOrElse(existingUser.username.validNec),
      user.password
        .map(newPassword => Validation.nonEmptyString("user.password")(newPassword))
        .sequence,
    ).mapN { (email, username, password) =>
      db.UpdateUser(
        email = email,
        username = username,
        password = password,
        // TODO: Current treatment of bio and image is problematic in case of nullifying those values as part of update
        // On the other hand specs don't tell anything explicitly about nullifying. I guess something like following
        // would make sense:
        // Omitting value in update request means "no change"
        // Specifying value to be null explicitly means "change the value to null"
        bio = user.bio.orElse(existingUser.bio),
        image = user.image.orElse(existingUser.image)
      )
    }
