package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.Entities.Articles
import pl.msitko.realworld.db.ArticleQuery

class ListArticlesServiceSpec extends PostgresSpec:
  test("rename me") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = new ArticleService(repos.articleRepo, repos.commentRepo, repos.followRepo, repos.userRepo)
    val followService  = new ProfileService(repos.followRepo, repos.userRepo)
    val userService    = new UserService(repos.userRepo, jwtConfig)

    for {
      t  <- userService.registration(registrationReqBody("user1"))
      t2 <- userService.registration(registrationReqBody("user2"))
      (user1Id, user2Id) = (t._1.id, t2._1.id)
      _ <- followService.followProfile(user2Id)("user1")

      _ <- articleService.createArticle(user1Id)(createArticleReqBody("title1"))
      _ <- articleService.createArticle(user1Id)(createArticleReqBody("title2", List("tag1")))
      _ <- articleService.createArticle(user1Id)(createArticleReqBody("title3", List("tag2")))
      _ <- articleService.createArticle(user1Id)(createArticleReqBody("title4", List("tag1", "tag2")))
      _ <- articleService.createArticle(user2Id)(createArticleReqBody("title5"))
      _ <- articleService.createArticle(user2Id)(createArticleReqBody("title6", List("tag1")))
      _ <- articleService.createArticle(user2Id)(createArticleReqBody("title7", List("tag2")))
      _ <- articleService.createArticle(user2Id)(createArticleReqBody("title8", List("tag1", "tag2")))
      _ <- articleService.favoriteArticle(user1Id)("title2")
      _ <- articleService.favoriteArticle(user1Id)("title7")
      articles1 <- articleService.listArticles(
        Some(user2Id),
        ArticleQuery(
          tag = Some("tag1")
        ),
        defaultPagination)
      _ <- IO(assertEquals(articles1.articles.map(_.title), List("title8", "title6", "title4", "title2")))
      articles2 <- articleService.listArticles(
        Some(user2Id),
        ArticleQuery(
          tag = Some("tag1"),
          author = Some("user1")
        ),
        defaultPagination)
      _ <- IO(assertEquals(articles2.articles.map(_.title), List("title4", "title2")))
      articles3 <- articleService.listArticles(
        Some(user2Id),
        ArticleQuery(
          favoritedBy = Some("user1"),
        ),
        defaultPagination)
      _ <- IO(assertEquals(articles3.articles.map(_.title), List("title7", "title2")))
      articles4 <- articleService.listArticles(
        Some(user2Id),
        ArticleQuery(
          favoritedBy = Some("user1"),
          tag = Some("tag2")
        ),
        defaultPagination)
      _ <- IO(assertEquals(articles4.articles.map(_.title), List("title7")))
      query = ArticleQuery(
        favoritedBy = Some("user1"),
        author = Some("user1"),
      )
      articles5 <- articleService.listArticles(Some(user2Id), query, defaultPagination)
      _         <- IO(assertEquals(articles5.articles.map(_.title), List("title2")))
      // it's safe to call head due to the above assertion
      articleFromRes5 = articles5.articles.head
      _ <- IO(assertEquals(articleFromRes5.favoritesCount, 1))
      _ <- IO(assertEquals(articleFromRes5.favorited, true))
      // articles6 is the same thing as article5 with the exception of articles6 being called without subject user
      articles6 <- articleService.listArticles(None, query, defaultPagination)
      _         <- IO(assertEquals(articles5.articles.map(_.title), List("title2")))
      // it's safe to call head due to the above assertion
      articleFromRes6 = articles6.articles.head
      _ <- IO(assertEquals(articleFromRes6.favoritesCount, 1))
      _ <- IO(assertEquals(articleFromRes6.favorited, false))
      articles7 <- articleService.listArticles(
        Some(user2Id),
        ArticleQuery(
          author = Some("user2"),
          favoritedBy = Some("user1"),
          tag = Some("tag2")
        ),
        defaultPagination)
      _ <- IO(assertEquals(articles7.articles.map(_.title), List("title7")))
    } yield ()
  }
