package pl.msitko.realworld.entities

import cats.data.ValidatedNec
import cats.syntax.all.*
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{ArticleId, ArticleNoId, UserId}
import pl.msitko.realworld.endpoints.ErrorInfo

import java.time.Instant

final case class Profile(username: String, bio: Option[String], image: Option[String], following: Boolean):
  def body: ProfileBody = ProfileBody(profile = this)

object Profile:
  def fromDB(author: db.Author) =
    Profile(
      username = author.username,
      bio = author.bio,
      image = author.image,
      following = author.following
    )

final case class ProfileBody(profile: Profile)

object ProfileBody:
  def fromDB(author: db.Author): ProfileBody = ProfileBody(Profile.fromDB(author))



final case class Tags(tags: List[String])
object Tags:
  def fromDB(tags: List[String]): Tags = Tags(tags)
