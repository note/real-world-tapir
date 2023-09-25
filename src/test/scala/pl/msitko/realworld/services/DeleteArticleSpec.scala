package pl.msitko.realworld.services

import cats.data.*
import cats.effect.IO
import cats.implicits.*
import pl.msitko.realworld.endpoints.ErrorInfo
import pl.msitko.realworld.entities.{AddCommentReq, AddCommentReqBody, ArticleBody}

class DeleteArticleSpec extends PostgresSpec {

  test("Article should be deleteable") {
    val transactor = createTransactor(postgres())
    val repos      = Repos.fromTransactor(transactor)

    val articleService = ArticleService(repos)
    val userService    = UserService(repos, jwtConfig)

    (for {
      t <- userService.registration(registrationReqBody("user1"))
      userId = t._1.id
      insertedArticle <- articleService.createArticle(userId)(createArticleReqBody("title", List("t1", "t2")))
      slug = insertedArticle.article.slug
      _ <- articleService.favoriteArticle(userId)(slug)
      commentReqBody = AddCommentReqBody(AddCommentReq(body = "some comment"))
      _ <- articleService.addComment(userId)(slug, commentReqBody)
      _ <- articleService.deleteArticle(userId)(slug)
      res <- articleService.getArticle(None)(slug).map(_.asRight[ErrorInfo]).recoverWith { case a =>
        EitherT.rightT[IO, ErrorInfo](Left(a))
      }
      _ <- ioAssertEquals(res, Left(ErrorInfo.NotFound))
    } yield ()).value
  }

}
