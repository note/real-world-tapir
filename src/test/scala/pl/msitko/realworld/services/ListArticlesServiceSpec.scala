package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.entities.OtherEntities.Articles
import pl.msitko.realworld.db.{ArticleQuery, Pagination}

class ListArticlesServiceSpec extends PostgresSpec:
  test("List articles should work for various filters") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = ArticleService(repos)
    val followService  = ProfileService(repos)
    val userService    = UserService(repos, jwtConfig)

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
      articles5 <- articleService.listArticles(Some(user1Id), query, defaultPagination)
      _         <- IO(assertEquals(articles5.articles.map(_.title), List("title2")))
      // it's safe to call head due to the above assertion
      articleFromRes5 = articles5.articles.head
      _         <- IO(assertEquals(articleFromRes5.favoritesCount, 1))
      _         <- IO(assertEquals(articleFromRes5.favorited, true))
      articles6 <- articleService.listArticles(Some(user2Id), query, defaultPagination)
      _         <- IO(assertEquals(articles6.articles.map(_.title), List("title2")))
      articleFromRes6 = articles6.articles.head
      _ <- IO(assertEquals(articleFromRes6.favoritesCount, 1))
      _ <- IO(assertEquals(articleFromRes6.favorited, false))
      // articles7 is the same thing as article5 with the exception of articles6 being called without subject user
      articles7 <- articleService.listArticles(None, query, defaultPagination)
      _         <- IO(assertEquals(articles5.articles.map(_.title), List("title2")))
      // it's safe to call head due to the above assertion
      articleFromRes7 = articles7.articles.head
      _ <- IO(assertEquals(articleFromRes7.favoritesCount, 1))
      _ <- IO(assertEquals(articleFromRes7.favorited, false))
      articles8 <- articleService.listArticles(
        Some(user2Id),
        ArticleQuery(
          author = Some("user2"),
          favoritedBy = Some("user1"),
          tag = Some("tag2")
        ),
        defaultPagination)
      _ <- IO(assertEquals(articles8.articles.map(_.title), List("title7")))
    } yield ()
  }

  test("Pagination should be taken into account") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = ArticleService(repos)

    def query(pagination: Pagination) =
      articleService.listArticles(
        None,
        ArticleQuery(
          tag = Some("tag1")
        ),
        pagination)

    for {
      articles1 <- query(Pagination(offset = 1, limit = 2))
      _         <- IO(assertEquals(articles1.articles.map(_.title), List("title6", "title4")))
      articles2 <- query(Pagination(offset = 2, limit = 2))
      _         <- IO(assertEquals(articles2.articles.map(_.title), List("title4", "title2")))
      articles3 <- query(Pagination(offset = 4, limit = 2))
      _         <- IO(assertEquals(articles3.articles, List.empty))
    } yield ()
  }
