package pl.msitko.realworld.services

import cats.data.NonEmptyList
import cats.effect.IO
import pl.msitko.realworld.Entities.{
  AddCommentReqBody,
  ArticleBody,
  Articles,
  Comment,
  CommentBody,
  Comments,
  CreateArticleReqBody,
  UpdateArticleReqBody
}
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{ArticleRepo, CommentRepo, FollowRepo, FullArticle, Pagination}
import pl.msitko.realworld.Entities
import pl.msitko.realworld.endpoints.ErrorInfo

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

class ArticleService(articleRepo: ArticleRepo, commentRepo: CommentRepo, followRepo: FollowRepo):
  def feedArticles(userId: UUID, pagination: Pagination): IO[Articles] =
    for {
      followed <- followRepo.getFollowedByUser(userId)
      r <- NonEmptyList.fromList(followed) match
        case Some(followedNel) =>
          articleRepo.feed(userId, followedNel, pagination)
        case None =>
          IO.pure(List.empty[FullArticle])
    } yield Articles(r.map(Entities.Article.fromDB))

  def getArticle(userIdOpt: Option[UUID])(slug: String): IO[Either[ErrorInfo, ArticleBody]] =
    withArticleOpt2(slug, userIdOpt)(dbArticle => ArticleBody.fromDB(dbArticle))

  def createArticle(userId: UUID)(reqBody: CreateArticleReqBody): IO[Either[ErrorInfo, ArticleBody]] =
    val dbArticle = reqBody.toDB(generateSlug(reqBody.article.title), Instant.now())
    articleRepo.insert(dbArticle, userId).flatMap(article => getArticleById(article.id, userId))

  def updateArticle(userId: UUID)(slug: String, reqBody: UpdateArticleReqBody): IO[Either[ErrorInfo, ArticleBody]] =
    withOwnedArticle(userId, slug) { existingArticle =>
      val changeObj = reqBody.toDB(reqBody.article.title.map(generateSlug), existingArticle.article)
      // This getArticleById is a bit lazy, we could avoid another DB query by composing existing and changeObj
      articleRepo
        .update(changeObj, existingArticle.article.id)
        .flatMap(_ => getArticleById(existingArticle.article.id, userId))
    }

  def deleteArticle(userId: UUID)(slug: String): IO[Either[ErrorInfo, Unit]] =
    withOwnedArticle(userId, slug) { _ =>
      articleRepo.delete(slug).map(_ => Right(()))
    }

  def addComment(userId: UUID)(slug: String, reqBody: AddCommentReqBody): IO[Either[ErrorInfo, CommentBody]] =
    withArticle(slug, userId) { article =>
      val comment = reqBody.toDB(userId, article.article.id, Instant.now)

      for {
        inserted   <- commentRepo.insert(comment)
        commentOpt <- commentRepo.getForCommentId(inserted.id, userId)
        res <- commentOpt match
          case Some(comment) =>
            IO.pure(Right(CommentBody.fromDB(comment)))
          case None =>
            IO.pure(Left(ErrorInfo.NotFound))
      } yield res
    }

  def getComments(userIdOpt: Option[UUID])(slug: String): IO[Either[ErrorInfo, Comments]] =
    withArticleOpt(slug, userIdOpt) { article =>
      commentRepo
        .getForArticleId(article.article.id, userIdOpt)
        .map(dbComments => Right(Comments(dbComments.map(Comment.fromDB))))
    }

  def deleteComment(userId: UUID)(commentId: Int): IO[Either[ErrorInfo, Unit]] =
    for {
      commentOpt <- commentRepo.getForCommentId(commentId, userId)
      res <- commentOpt match
        case Some(comment) if comment.comment.authorId == userId =>
          commentRepo.delete(commentId).map(_ => Right(()))
        case Some(comment) =>
          IO.pure(Left(ErrorInfo.Unauthorized))
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
    } yield res

  def favoriteArticle(userId: UUID)(slug: String): IO[Either[ErrorInfo, ArticleBody]] =
    withArticle(slug, userId) { article =>
      val articleBody = ArticleBody.fromDB(article)
      val updatedArticle = articleBody.copy(article =
        articleBody.article.copy(favorited = true, favoritesCount = articleBody.article.favoritesCount + 1))
      articleRepo.insertFavorite(article.article.id, userId).map(_ => Right(updatedArticle))
    }

  def unfavoriteArticle(userId: UUID)(slug: String): IO[Either[ErrorInfo, ArticleBody]] =
    withArticle(slug, userId) { article =>
      for {
        _              <- articleRepo.deleteFavorite(article.article.id, userId)
        updatedArticle <- getArticleById(article.article.id, userId)
      } yield updatedArticle
    }

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

  private def getBySlug(slug: String, subjectUserId: Option[UUID]) =
    subjectUserId match
      case Some(uid) => articleRepo.getBySlug(slug, uid)
      case None      => articleRepo.getBySlug(slug)

  private def withArticleOpt[T](slug: String, subjectUserId: Option[UUID])(
      fn: db.FullArticle => IO[Either[ErrorInfo, T]]): IO[Either[ErrorInfo, T]] =
    for {
      articleOption <- getBySlug(slug, subjectUserId)
      res <- articleOption match
        case Some(article) =>
          fn(article)
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
    } yield res

  private def withArticleOpt2[T](slug: String, subjectUserId: Option[UUID])(
      fn: db.FullArticle => T): IO[Either[ErrorInfo, T]] =
    for {
      articleOption <- getBySlug(slug, subjectUserId)
      res <- articleOption match
        case Some(article) =>
          IO.pure(Right(fn(article)))
        case None =>
          IO.pure(Left(ErrorInfo.NotFound))
    } yield res

  private def withArticle[T](slug: String, subjectUserId: UUID)(
      fn: db.FullArticle => IO[Either[ErrorInfo, T]]): IO[Either[ErrorInfo, T]] =
    withArticleOpt(slug, Some(subjectUserId))(fn)
