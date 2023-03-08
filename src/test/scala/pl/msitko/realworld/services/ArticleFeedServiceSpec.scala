package pl.msitko.realworld.services

import cats.effect.IO
import munit.CatsEffectSuite
import pl.msitko.realworld.Entities.*
import pl.msitko.realworld.db.Pagination

class ArticleFeedServiceSpec extends PostgresSpec:

  test("Feed should return multiple articles created by followed users, ordered by most recent first") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = ArticleService(repos)
    val followService  = ProfileService(repos)
    val userService    = UserService(repos, jwtConfig)

    for {
      t  <- userService.registration(registrationReqBody("user1"))
      t2 <- userService.registration(registrationReqBody("user2"))
      t3 <- userService.registration(registrationReqBody("user3"))
      (user1Id, user2Id, user3Id) = (t._1.id, t2._1.id, t3._1.id)
      _     <- articleService.createArticle(user1Id)(createArticleReqBody("title1"))
      _     <- articleService.createArticle(user2Id)(createArticleReqBody("title2"))
      _     <- articleService.createArticle(user3Id)(createArticleReqBody("title3"))
      _     <- articleService.createArticle(user3Id)(createArticleReqBody("title4"))
      _     <- articleService.createArticle(user3Id)(createArticleReqBody("title5"))
      feed1 <- articleService.feedArticles(user2Id, defaultPagination)
      _     <- IO(assertEquals(feed1, Articles.fromArticles(List.empty)))
      _     <- followService.followProfile(user2Id)("user3")
      feed2 <- articleService.feedArticles(user2Id, defaultPagination)
      _     <- IO(assertEquals(feed2.articles.map(_.title), List("title5", "title4", "title3")))
    } yield ()
  }

  test("Feed should take offset and limit into account") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = ArticleService(repos)
    val followService  = ProfileService(repos)
    val userService    = UserService(repos, jwtConfig)

    for {
      t  <- userService.registration(registrationReqBody("u1"))
      t2 <- userService.registration(registrationReqBody("u2"))
      (user1Id, user2Id) = (t._1.id, t2._1.id)
      _     <- articleService.createArticle(user1Id)(createArticleReqBody("aTitle1"))
      _     <- articleService.createArticle(user1Id)(createArticleReqBody("aTitle2"))
      _     <- articleService.createArticle(user1Id)(createArticleReqBody("aTitle3"))
      _     <- articleService.createArticle(user1Id)(createArticleReqBody("aTitle4"))
      _     <- articleService.createArticle(user1Id)(createArticleReqBody("aTitle5"))
      _     <- followService.followProfile(user2Id)("u1")
      feed1 <- articleService.feedArticles(user2Id, defaultPagination)
      _ <- IO(assertEquals(feed1.articles.map(_.title), List("aTitle5", "aTitle4", "aTitle3", "aTitle2", "aTitle1")))
      feed2 <- articleService.feedArticles(user2Id, Pagination(offset = 0, limit = 2))
      _     <- IO(assertEquals(feed2.articles.map(_.title), List("aTitle5", "aTitle4")))
      feed3 <- articleService.feedArticles(user2Id, Pagination(offset = 2, limit = 2))
      _     <- IO(assertEquals(feed3.articles.map(_.title), List("aTitle3", "aTitle2")))
    } yield ()

  }
