package pl.msitko.realworld.services

import cats.data.{EitherT, NonEmptyList}
import pl.msitko.realworld.entities._

class UpdateArticleSpec extends PostgresSpec:
  test("slug generation should happen if changing title to already existing one") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = ArticleService(repos)
    val tagsService    = TagService(repos)
    val userService    = UserService(repos, jwtConfig)

    (for {
      t <- userService.registration(registrationReqBody("userA"))
      user1Id = t._1.id
      _        <- articleService.createArticle(user1Id)(createArticleReqBody("title1"))
      article2 <- articleService.createArticle(user1Id)(createArticleReqBody("title2"))
      update = UpdateArticleReqBody(article = UpdateArticleReq(title = Some("title1"), description = None, body = None))
      updatedArticle <- articleService.updateArticle(user1Id)(article2.article.slug, update)
      // The idea here is that article with slug="title1" already exists. Then, we expect the slug to be suffixed with
      // s"-${7-random-alphanumeric-chars}"
      _ <- ioAssertEquals(updatedArticle.article.slug.size, "title1-1234567".size)
    } yield ()).value

  }
