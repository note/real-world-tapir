package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.Entities.ArticleBody
import pl.msitko.realworld.db
import pl.msitko.realworld.db.ArticleRepo
import pl.msitko.realworld.{Entities, ExampleResponses, JwtConfig}
import pl.msitko.realworld.endpoints.{ArticleEndpoints, ErrorInfo}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

class ArticleServices(repo: ArticleRepo, jwtConfig: JwtConfig):
  val articleEndpoints = new ArticleEndpoints(jwtConfig)

  val listArticlesImpl =
    articleEndpoints.listArticles.serverLogicSuccess(_ => IO.pure(Entities.Articles(articles = List.empty)))

  val feedArticlesImpl =
    articleEndpoints.feedArticles.serverLogicSuccess(_ => IO.pure(Entities.Articles(articles = List.empty)))

  val getArticleImpl =
    articleEndpoints.getArticle.serverLogicOption { slug =>
      for {
        article <- repo.getBySlug(slug)
        httpArticle = article.map(ArticleBody.fromDB)
      } yield httpArticle
    }

  val createArticleImpl =
    articleEndpoints.createArticle.serverLogicSuccess { userId => reqBody =>
      val dbArticle = reqBody.toDB(generateSlug(reqBody.article.title), Instant.now())
      repo.insert(dbArticle, userId).map(ArticleBody.fromDB)
    }

  val updateArticleImpl =
    articleEndpoints.updateArticle.serverLogic { userId => (slug, reqBody) =>
      for {
        articleOption <- repo.getBySlug(slug)
        res <- articleOption match
          case Some(existingArticle) if existingArticle.authorId == userId =>
            val changeObj = reqBody.toDB(reqBody.article.title.map(generateSlug), existingArticle)
            // This getArticleById is a bit lazy, we could avoid another DB query by composing existing and changeObj
            repo.update(changeObj, existingArticle.id).flatMap(_ => getArticleById(existingArticle.id))
          case Some(_) =>
            IO.pure(Left(ErrorInfo.Unauthorized))
          case None =>
            IO.pure(Left(ErrorInfo.NotFound))
      } yield res
    }

  private def getArticleById(id: UUID): IO[Either[ErrorInfo, ArticleBody]] =
    repo.getById(id).map {
      case Some(article) => Right(ArticleBody.fromDB(article))
      case None          => Left(ErrorInfo.NotFound)
    }

  val deleteArticleImpl =
    articleEndpoints.deleteArticle.serverLogic { userId => slug =>
      for {
        articleOption <- repo.getBySlug(slug)
        res <- articleOption match
          case Some(a) if a.authorId == userId =>
            repo.delete(slug).map(_ => Right(()))
          case Some(_) =>
            IO.pure(Left(ErrorInfo.Unauthorized))
          case None =>
            IO.pure(Left(ErrorInfo.NotFound))
      } yield res
    }

  val addCommentImpl =
    articleEndpoints.addComment.serverLogicSuccess(slug => IO.pure(ExampleResponses.commentBody))

  val getCommentsImpl =
    articleEndpoints.getComments.serverLogicSuccess(slug =>
      IO.pure(Entities.Comments(comments = List(ExampleResponses.comment))))

  val deleteCommentImpl =
    articleEndpoints.deleteComment.serverLogicSuccess((slug, commentId) => IO.pure(()))

  val favoriteArticleImpl =
    articleEndpoints.favoriteArticle.serverLogicSuccess(slug => IO.pure(ExampleResponses.articleBody))

  val unfavoriteArticleImpl =
    articleEndpoints.unfavoriteArticle.serverLogicSuccess(slug => IO.pure(ExampleResponses.articleBody))

  def services = List(
    listArticlesImpl,
    feedArticlesImpl,
    getArticleImpl,
    createArticleImpl,
    updateArticleImpl,
    deleteArticleImpl,
    addCommentImpl,
    getCommentsImpl,
    deleteCommentImpl,
    favoriteArticleImpl,
    unfavoriteArticleImpl,
  )

  // TODO: Replace with proper implementation
  private def generateSlug(title: String): String =
    URLEncoder.encode(title, StandardCharsets.UTF_8)
