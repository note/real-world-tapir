package pl.msitko.realworld.services

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import pl.msitko.realworld.entities.{
  AddCommentReqBody,
  Article,
  ArticleBody,
  Articles,
  Comment,
  CommentBody,
  Comments,
  CreateArticleReqBody,
  UpdateArticleReqBody
}
import pl.msitko.realworld.*
import pl.msitko.realworld.db
import pl.msitko.realworld.db.{ArticleId, ArticleRepo, CommentRepo, FollowRepo, TagId, TagRepo, UserId, UserRepo}
import pl.msitko.realworld.endpoints.ErrorInfo

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

object ArticleService:
  def apply(repos: Repos): ArticleService =
    new ArticleService(repos.articleRepo, repos.commentRepo, repos.followRepo, repos.userRepo, repos.tagRepo)

class ArticleService(
    articleRepo: ArticleRepo,
    commentRepo: CommentRepo,
    followRepo: FollowRepo,
    userRepo: UserRepo,
    tagRepo: TagRepo):
  def feedArticles(userId: UserId, pagination: db.Pagination): IO[Articles] =
    for {
      followed <- followRepo.getFollowedByUser(userId)
      r <- NonEmptyList.fromList(followed) match
        case Some(followedNel) =>
          articleRepo.feed(userId, followedNel, pagination)
        case None =>
          IO.pure(List.empty[db.FullArticle])
    } yield Articles.fromArticles(r.map(Article.fromDB))

  def listArticles(
      subjectUserId: Option[UserId],
      query: db.ArticleQuery[String],
      pagination: db.Pagination): IO[Articles] =
    val resolvedQuery = query.favoritedBy match
      case Some(username) =>
        userRepo.resolveUsername(username).map {
          case Some(favoritedByUserId) =>
            db.ArticleQuery[db.UserCoordinates](
              tag = query.tag,
              author = query.author,
              favoritedBy = Some(db.UserCoordinates(username, favoritedByUserId)))
          case None =>
            // If username cannot be resolved we ignore query.favoritedBy
            db.ArticleQuery[db.UserCoordinates](tag = query.tag, author = query.author, favoritedBy = None)
        }
      case None =>
        IO.pure(db.ArticleQuery[db.UserCoordinates](tag = query.tag, author = query.author, favoritedBy = None))

    resolvedQuery.flatMap { rq =>
      articleRepo
        .listArticles(rq, pagination, subjectUserId)
        .map(articles => Articles.fromArticles(articles.map(Article.fromDB)))
    }

  def getArticle(userIdOpt: Option[UserId])(slug: String): Result[ArticleBody] =
    getArticleBySlug(slug, userIdOpt).map(dbArticle => ArticleBody.fromDB(dbArticle))

  def createArticle(userId: UserId)(reqBody: CreateArticleReqBody): Result[ArticleBody] =
    for {
      dbArticle <- reqBody.toDB(generateSlug(reqBody.article.title), Instant.now()).toResult
      tagIds    <- insertTags(dbArticle.tags)
      article   <- insertArticle(dbArticle, userId)
      articleTags = tagIds.map(tagId => db.ArticleTag(articleId = article.article.id, tagId = tagId))
      _ <- insertArticleTags(articleTags)
    } yield ArticleBody.fromDB(article)

  private def insertArticle(article: db.ArticleNoId, userId: UserId): Result[db.FullArticle] =
    for {
      article <- EitherT.right(articleRepo.insert(article, userId))
      fetched <- getArticleById(article.id, userId)
    } yield fetched

  private def insertTags(tags: List[String]): Result[List[TagId]] =
    EitherT.right[ErrorInfo](
      NonEmptyList.fromList(tags) match
        case Some(ts) => tagRepo.upsertTags(ts)
        case None     => IO.pure(List.empty)
    )

  private def insertArticleTags(articleTags: List[db.ArticleTag]): Result[Int] =
    EitherT.right[ErrorInfo](
      NonEmptyList.fromList(articleTags) match
        case Some(ts) => tagRepo.insertArticleTags(ts)
        case None     => IO.pure(0)
    )

  def updateArticle(userId: UserId)(slug: String, reqBody: UpdateArticleReqBody): Result[ArticleBody] =
    for {
      existingArticle <- getOwnedArticle(slug, userId)
      changeObj = reqBody.toDB(reqBody.article.title.map(generateSlug), existingArticle.article)
      _ <- EitherT.right(articleRepo.update(changeObj, existingArticle.article.id))
      // This getArticleBodyById is a bit lazy, we could avoid another DB query by composing existing and changeObj
      fetchedArticle <- getArticleBodyById(existingArticle.article.id, userId)
    } yield fetchedArticle

  def deleteArticle(userId: UserId)(slug: String): Result[Unit] =
    for {
      _ <- getOwnedArticle(slug, userId)
      _ <- EitherT.right(articleRepo.delete(slug))
    } yield ()

  def addComment(userId: UserId)(slug: String, reqBody: AddCommentReqBody): Result[CommentBody] =
    for {
      article    <- getArticleBySlug(slug, userId)
      comment    <- reqBody.toDB(userId, article.article.id, Instant.now).toResult
      inserted   <- EitherT.right(commentRepo.insert(comment))
      commentOpt <- EitherT.right(commentRepo.getForCommentId(inserted.id, userId))
      res <- commentOpt match
        case Some(comment) =>
          EitherT.rightT[IO, ErrorInfo](CommentBody.fromDB(comment))
        case None =>
          EitherT.leftT[IO, CommentBody](ErrorInfo.NotFound)
    } yield res

  def getComments(userIdOpt: Option[UserId])(slug: String): Result[Comments] =
    for {
      article <- getArticleBySlug(slug, userIdOpt)
      comments <- EitherT.right(
        commentRepo
          .getForArticleId(article.article.id, userIdOpt)
          .map(dbComments => Comments(dbComments.map(Comment.fromDB))))
    } yield comments

  def deleteComment(userId: UserId)(commentId: Int): Result[Unit] =
    EitherT(
      for {
        commentOpt <- commentRepo.getForCommentId(commentId, userId)
        res <- commentOpt match
          case Some(comment) if comment.comment.authorId == userId =>
            commentRepo.delete(commentId).map(_ => Right(()))
          case Some(comment) =>
            IO.pure(Left(ErrorInfo.Unauthorized))
          case None =>
            IO.pure(Left(ErrorInfo.NotFound))
      } yield res)

  def favoriteArticle(userId: UserId)(slug: String): Result[ArticleBody] =
    for {
      article <- getArticleBySlug(slug, userId)
      articleBody = ArticleBody.fromDB(article)
      updatedArticle = articleBody.copy(article =
        articleBody.article.copy(favorited = true, favoritesCount = articleBody.article.favoritesCount + 1))
      _ <- EitherT.right(articleRepo.insertFavorite(article.article.id, userId))
    } yield updatedArticle

  def unfavoriteArticle(userId: UserId)(slug: String): Result[ArticleBody] =
    for {
      article        <- getArticleBySlug(slug, userId)
      updatedArticle <- getArticleBodyById(article.article.id, userId)
    } yield updatedArticle

  // TODO: Replace with proper implementation
  private def generateSlug(title: String): String =
    URLEncoder.encode(title, StandardCharsets.UTF_8)

  private def getArticleById(articleId: ArticleId, userId: UserId): Result[db.FullArticle] =
    EitherT(articleRepo.getById(articleId, userId).map {
      case Some(article) => Right(article)
      case None          => Left(ErrorInfo.NotFound)
    })

  private def getArticleBodyById(articleId: ArticleId, userId: UserId): Result[ArticleBody] =
    getArticleById(articleId, userId).map(ArticleBody.fromDB)

  private def getOwnedArticle(slug: String, userId: UserId): Result[db.FullArticle] =
    for {
      article <- getArticleBySlug(slug, userId)
      res <- article match
        case a if a.article.authorId == userId =>
          EitherT.rightT[IO, ErrorInfo](a)
        case _ =>
          EitherT.leftT[IO, db.FullArticle](ErrorInfo.Unauthorized)
    } yield res

  private def getBySlug(slug: String, subjectUserId: Option[UserId]) =
    subjectUserId match
      case Some(uid) => articleRepo.getBySlug(slug, uid)
      case None      => articleRepo.getBySlug(slug)

  private def getArticleBySlug(slug: String, subjectUserId: Option[UserId]): Result[db.FullArticle] =
    for {
      articleOption <- EitherT.right(getBySlug(slug, subjectUserId))
      res <- articleOption match
        case Some(article) =>
          EitherT.rightT[IO, ErrorInfo](article)
        case None =>
          EitherT.leftT[IO, db.FullArticle](ErrorInfo.NotFound)
    } yield res

  private def getArticleBySlug(slug: String, subjectUserId: UserId): Result[db.FullArticle] =
    getArticleBySlug(slug, Some(subjectUserId))
