package pl.msitko.realworld.services

import cats.data.{EitherT, NonEmptyList}
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
import pl.msitko.realworld.db.{
  ArticleQuery,
  ArticleRepo,
  ArticleTag,
  CommentRepo,
  FollowRepo,
  FullArticle,
  Pagination,
  TagRepo,
  UserCoordinates,
  UserRepo
}
import pl.msitko.realworld.Entities
import pl.msitko.realworld.endpoints.ErrorInfo

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

object ArticleService:
  def apply(repos: Repos): ArticleService =
    new ArticleService(repos.articleRepo, repos.commentRepo, repos.followRepo, repos.userRepo, repos.tagRepo)

class ArticleService(
    articleRepo: ArticleRepo,
    commentRepo: CommentRepo,
    followRepo: FollowRepo,
    userRepo: UserRepo,
    tagRepo: TagRepo):
  def feedArticles(userId: UUID, pagination: Pagination): IO[Articles] =
    for {
      followed <- followRepo.getFollowedByUser(userId)
      r <- NonEmptyList.fromList(followed) match
        case Some(followedNel) =>
          articleRepo.feed(userId, followedNel, pagination)
        case None =>
          IO.pure(List.empty[FullArticle])
    } yield Articles(r.map(Entities.Article.fromDB))

  def listArticles(subjectUserId: Option[UUID], query: ArticleQuery[String], pagination: Pagination): IO[Articles] =
    val resolvedQuery = query.favoritedBy match
      case Some(username) =>
        userRepo.resolveUsername(username).map {
          case Some(favoritedByUserId) =>
            ArticleQuery[UserCoordinates](
              tag = query.tag,
              author = query.author,
              favoritedBy = Some(UserCoordinates(username, favoritedByUserId)))
          case None =>
            // If username cannot be resolved we ignore query.favoritedBy
            ArticleQuery[UserCoordinates](tag = query.tag, author = query.author, favoritedBy = None)
        }
      case None =>
        IO.pure(ArticleQuery[UserCoordinates](tag = query.tag, author = query.author, favoritedBy = None))

    resolvedQuery.flatMap { rq =>
      articleRepo
        .listArticles(rq, pagination, subjectUserId)
        .map(articles => Articles(articles.map(Entities.Article.fromDB)))
    }

  def getArticle(userIdOpt: Option[UUID])(slug: String): IO[Either[ErrorInfo, ArticleBody]] =
    withArticleOpt2(slug, userIdOpt)(dbArticle => ArticleBody.fromDB(dbArticle))

  def createArticle(userId: UUID)(reqBody: CreateArticleReqBody): IO[Either[ErrorInfo, ArticleBody]] =
    val dbArticle = reqBody.toDB(generateSlug(reqBody.article.title), Instant.now())

    (for {
      tagIds  <- insertTags(dbArticle.tags)
      article <- insertArticle(dbArticle, userId)
      articleTags = tagIds.map(tagId => ArticleTag(articleId = article.article.id, tagId = tagId))
      _ <- insertArticleTags(articleTags)
    } yield ArticleBody.fromDB(article)).value

  private def insertArticle(article: db.ArticleNoId, userId: UUID): EitherT[IO, ErrorInfo, db.FullArticle] =
    EitherT(
      articleRepo.insert(article, userId).flatMap(article => getArticleById(article.id, userId))
    )

  private def insertTags(tags: List[String]): EitherT[IO, ErrorInfo, List[UUID]] =
    EitherT.right[ErrorInfo](
      NonEmptyList.fromList(tags) match
        case Some(ts) => tagRepo.upsertTags(ts)
        case None     => IO.pure(List.empty)
    )

  private def insertArticleTags(articleTags: List[db.ArticleTag]): EitherT[IO, ErrorInfo, Int] =
    EitherT.right[ErrorInfo](
      NonEmptyList.fromList(articleTags) match
        case Some(ts) => tagRepo.insertArticleTags(ts)
        case None     => IO.pure(0)
    )

  def updateArticle(userId: UUID)(slug: String, reqBody: UpdateArticleReqBody): IO[Either[ErrorInfo, ArticleBody]] =
    withOwnedArticle(userId, slug) { existingArticle =>
      val changeObj = reqBody.toDB(reqBody.article.title.map(generateSlug), existingArticle.article)
      // This getArticleBodyById is a bit lazy, we could avoid another DB query by composing existing and changeObj
      articleRepo
        .update(changeObj, existingArticle.article.id)
        .flatMap(_ => getArticleBodyById(existingArticle.article.id, userId))
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
        updatedArticle <- getArticleBodyById(article.article.id, userId)
      } yield updatedArticle
    }

  // TODO: Replace with proper implementation
  private def generateSlug(title: String): String =
    URLEncoder.encode(title, StandardCharsets.UTF_8)

  private def getArticleById(articleId: UUID, userId: UUID): IO[Either[ErrorInfo, db.FullArticle]] =
    articleRepo.getById(articleId, userId).map {
      case Some(article) => Right(article)
      case None          => Left(ErrorInfo.NotFound)
    }

  private def getArticleBodyById(articleId: UUID, userId: UUID): IO[Either[ErrorInfo, ArticleBody]] =
    getArticleById(articleId, userId).map(_.map(ArticleBody.fromDB))

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
