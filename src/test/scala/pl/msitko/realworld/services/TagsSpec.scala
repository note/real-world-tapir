package pl.msitko.realworld.services

import cats.data.{EitherT, NonEmptyList}

class TagsSpec extends PostgresSpec:
  test("After inserting 2 articles with the same tags, each tag should appear in the DB just once") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = ArticleService(repos)
    val tagsService    = TagService(repos)
    val userService    = UserService(repos, jwtConfig)

    (for {
      t <- userService.registration(registrationReqBody("userA"))
      user1Id = t._1.id
      _    <- articleService.createArticle(user1Id)(createArticleReqBody("title1", List("tag1", "tag2")))
      _    <- articleService.createArticle(user1Id)(createArticleReqBody("title2", List("tag1", "tag2")))
      tags <- EitherT.right(tagsService.getTags)
      _    <- ioAssertEquals(tags.tags.size, 2)
    } yield ()).value

  }
