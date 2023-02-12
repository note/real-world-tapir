package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.Entities.{ArticleBody, Comment, CommentBody, Comments}
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{ArticleRepo, CommentRepo, FullComment}
import pl.msitko.realworld.{Entities, ExampleResponses, JwtConfig}
import pl.msitko.realworld.endpoints.{ArticleEndpoints, ErrorInfo}

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

class ArticleServices(articleRepo: ArticleRepo, commentRepo: CommentRepo, jwtConfig: JwtConfig):
  val articleEndpoints = new ArticleEndpoints(jwtConfig)

  val listArticlesImpl =
    articleEndpoints.listArticles.serverLogicSuccess(_ => IO.pure(Entities.Articles(articles = List.empty)))

  val feedArticlesImpl =
    articleEndpoints.feedArticles.serverLogicSuccess(_ => _ => IO.pure(Entities.Articles(articles = List.empty)))

  val getArticleImpl =
    articleEndpoints.getArticle.serverLogic { userIdOpt => slug =>
      userIdOpt match
        case Some(userId) =>
          withArticle2(slug, userId)(dbArticle => ArticleBody.fromDB(dbArticle))
        case None =>
          withArticle2(slug)(dbArticle => ArticleBody.fromDB(dbArticle))
    }

  val createArticleImpl =
    articleEndpoints.createArticle.serverLogic { userId => reqBody =>
      val dbArticle = reqBody.toDB(generateSlug(reqBody.article.title), Instant.now())
      articleRepo.insert(dbArticle, userId).flatMap(article => getArticleById(article.id, userId))
    }

  val updateArticleImpl =
    articleEndpoints.updateArticle.serverLogic { userId => (slug, reqBody) =>
      withOwnedArticle(userId, slug) { existingArticle =>
        val changeObj = reqBody.toDB(reqBody.article.title.map(generateSlug), existingArticle.article)
        // This getArticleById is a bit lazy, we could avoid another DB query by composing existing and changeObj
        articleRepo
          .update(changeObj, existingArticle.article.id)
          .flatMap(_ => getArticleById(existingArticle.article.id, userId))
      }
    }

  val deleteArticleImpl =
    articleEndpoints.deleteArticle.serverLogic { userId => slug =>
      withOwnedArticle(userId, slug) { _ =>
        articleRepo.delete(slug).map(_ => Right(()))
      }
    }

  val addCommentImpl =
    articleEndpoints.addComment.serverLogic { userId => (slug, reqBody) =>
      withArticle(slug) { article =>
        val comment = reqBody.toDB(userId, article.article.id, Instant.now)

        commentRepo.insert(comment).map(c => FullComment(c, article.author))

        for {
          inserted   <- commentRepo.insert(comment)
          commentOpt <- commentRepo.getForCommentId(inserted.id)
          res <- commentOpt match
            case Some(comment) =>
              IO.pure(Right(CommentBody.fromDB(comment)))
            case None =>
              IO.pure(Left(ErrorInfo.NotFound))
        } yield res
      }
    }

  val getCommentsImpl =
    articleEndpoints.getComments.serverLogic { userIdOpt => slug =>
      withArticle(slug) { article =>
        commentRepo
          .getForArticleId(article.article.id)
          .map(dbComments => Right(Comments(dbComments.map(Comment.fromDB))))
      }
    }

  val deleteCommentImpl =
    articleEndpoints.deleteComment.serverLogic { userId => (slug, commentId) =>
      for {
        commentOpt <- commentRepo.getForCommentId(commentId)
        res <- commentOpt match
          case Some(comment) if comment.comment.authorId == userId =>
            commentRepo.delete(commentId).map(_ => Right(()))
          case Some(comment) =>
            IO.pure(Left(ErrorInfo.Unauthorized))
          case None =>
            IO.pure(Left(ErrorInfo.NotFound))
      } yield res
    }

  val favoriteArticleImpl =
    articleEndpoints.favoriteArticle.serverLogic { userId => slug =>
      withArticle(slug, userId) { article =>
        val articleBody = ArticleBody.fromDB(article)
        val updatedArticle = articleBody.copy(article =
          articleBody.article.copy(favorited = true, favoritesCount = articleBody.article.favoritesCount + 1))
        articleRepo.insertFavorite(article.article.id, userId).map(_ => Right(updatedArticle))
      }
    }

  val unfavoriteArticleImpl =
    articleEndpoints.unfavoriteArticle.serverLogic { userId => slug =>
      withArticle(slug, userId) { article =>
        for {
          _              <- articleRepo.deleteFavorite(article.article.id, userId)
          updatedArticle <- getArticleById(article.article.id, userId)
        } yield updatedArticle
      }
    }

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

  private def getArticleById(articleId: UUID, userId: UUID): IO[Either[ErrorInfo, ArticleBody]] =
    articleRepo.getById(articleId, userId).map {
      case Some(article) => Right(ArticleBody.fromDB(article))
      case None          => Left(ErrorInfo.NotFound)
    }

  private def withOwnedArticle[T](userId: UUID, slug: String)(
      fn: db.FullArticle => IO[Either[ErrorInfo, T]]): IO[Either[ErrorInfo, T]] =
    for {
      articleOption <- articleRepo.getBySlug(slug)
      res <- articleOption match
        case Some(a) if a.article.authorId == userId =>
          fn(a)
        case Some(_) =>
          IO.pure(Left(ErrorInfo.Unauthorized))
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
    } yield res

  private def withArticle[T](slug: String)(fn: db.FullArticle => IO[Either[ErrorInfo, T]]): IO[Either[ErrorInfo, T]] =
    for {
      articleOption <- articleRepo.getBySlug(slug)
      res <- articleOption match
        case Some(article) =>
          fn(article)
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
    } yield res

  // TODO: rename
  private def withArticle2[T](slug: String)(fn: db.FullArticle => T): IO[Either[ErrorInfo, T]] =
    for {
      articleOption <- articleRepo.getBySlug(slug)
      res <- articleOption match
        case Some(article) =>
          IO.pure(Right(fn(article)))
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
    } yield res

  private def withArticle[T](slug: String, subjectUserId: UUID)(
      fn: db.FullArticle => IO[Either[ErrorInfo, T]]): IO[Either[ErrorInfo, T]] =
    for {
      articleOption <- articleRepo.getBySlug(slug, subjectUserId)
      res <- articleOption match
        case Some(article) =>
          fn(article)
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
    } yield res

  // TODO: rename
  private def withArticle2[T](slug: String, subjectUserId: UUID)(fn: db.FullArticle => T): IO[Either[ErrorInfo, T]] =
    for {
      articleOption <- articleRepo.getBySlug(slug, subjectUserId)
      res <- articleOption match
        case Some(article) =>
          IO.pure(Right(fn(article)))
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
    } yield res
