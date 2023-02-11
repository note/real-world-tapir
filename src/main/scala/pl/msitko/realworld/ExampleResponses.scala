package pl.msitko.realworld

import cats.implicits.*

import java.time.Instant

object ExampleResponses:
  val user: Entities.User = Entities.User(
    email = "a@example.com",
    token = "abc",
    username = "user",
    bio = None,
    image = None
  )
  val userBody: Entities.UserBody = user.body

  def profile(username: String) =
    Entities.Profile(
      username = username,
      bio = "I work at statefarm",
      image = "https://api.realworld.io/images/smiley-cyrus.jpg".some,
      following = false
    )

  def profileBody(username: String) =
    profile(username).body

  val article = Entities.Article(
    slug = "slug",
    title = "How ta train your dragon",
    description = "Ever wonder how?",
    body = "abc",
    tagList = List("abc"),
    createdAt = Instant.now(),
    updatedAt = Instant.now(),
    favorited = false,
    favoritesCount = 3,
    author = profile("someone")
  )
  val articleBody = article.toBody

  val comment = Entities.Comment(
    id = 2,
    createdAt = Instant.now,
    updatedAt = Instant.now,
    body = "It takes a Jacobian",
    author = profile("someone")
  )
  val commentBody = comment.toBody
