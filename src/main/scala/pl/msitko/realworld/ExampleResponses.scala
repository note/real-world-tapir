package pl.msitko.realworld

import cats.implicits.*
import pl.msitko.realworld.Entities.ProfileBody

import java.time.Instant

object ExampleResponses:
  def profile(username: String): Entities.Profile =
    Entities.Profile(
      username = username,
      bio = "I work at statefarm".some,
      image = "https://api.realworld.io/images/smiley-cyrus.jpg".some,
      following = false
    )

  def profileBody(username: String): Entities.ProfileBody =
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
