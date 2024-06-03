package pl.msitko.realworld.db

import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.implicits.legacy.instant.*
import pl.msitko.realworld.entities
import pl.msitko.realworld.entities.{Profile, ProfileBody}
import pl.msitko.realworld.{db, Validated, Validation}
import sttp.model.Uri

import java.time.Instant

final case class ArticleNoId(
    slug: String,
    title: String,
    description: String,
    body: String,
    tags: List[String],
    createdAt: Instant,
    updatedAt: Instant,
)

object ArticleNoId:
  def fromHttp(req: entities.CreateArticleReqBody, slug: String, now: Instant): Validated[ArticleNoId] =
    (
      Validation.nonEmptyString("article.title")(req.article.title),
      Validation.nonEmptyString("article.description")(req.article.description),
      Validation.nonEmptyString("article.body")(req.article.body)).mapN { (title, description, body) =>
      db.ArticleNoId(
        slug = slug,
        title = title,
        description = description,
        body = body,
        tags = req.article.tagList,
        createdAt = now,
        updatedAt = now,
      )
    }

final case class Article(
    id: ArticleId,
    slug: String,
    title: String,
    description: String,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
    authorId: UserId,
)

// FullArticle represents a result row of data related to a single article and JOINed over many tables
final case class FullArticle(
    article: Article,
    author: Author,
    favoritesCount: Option[Int],
    favorited: Option[Int],
    tags: List[String]
):
  def toHttp: entities.Article =
    entities.Article(
      slug = Uri.unsafeParse(article.slug).toString,
      title = article.title,
      description = article.description,
      body = article.body,
      tagList = tags,
      createdAt = article.createdAt,
      updatedAt = article.updatedAt,
      favorited = favorited.isDefined,
      favoritesCount = favoritesCount.getOrElse(0),
      author = author.toHttpProfile
    )

object FullArticle:
  implicit val fullArticleRead: Read[FullArticle] =
    Read[(
        ArticleId,
        String,
        String,
        String,
        String,
        Instant,
        Instant,
        UserId,
        String,
        Option[String],
        Option[String],
        Option[Int],
        Option[Int],
        Option[Int],
        Option[String])]
      .map {
        case (
              id,
              slug,
              title,
              description,
              body,
              createdAt,
              updatedAt,
              authorId,
              authorUsername,
              authorBio,
              image,
              favoritesCount,
              favorited,
              following,
              tags) =>
          val article = Article(
            id = id,
            slug = slug,
            title = title,
            description = description,
            body = body,
            createdAt = createdAt,
            updatedAt = updatedAt,
            authorId = authorId)
          FullArticle(
            article = article,
            author = Author(
              username = authorUsername,
              bio = authorBio,
              image = image,
              following = following.isDefined,
            ),
            favoritesCount = favoritesCount,
            favorited = favorited,
            // TODO: document and enforce no commas in tag names
            tags = tags.map(_.split(',').toList).getOrElse(List.empty)
          )
      }

final case class Author(
    username: String,
    bio: Option[String],
    image: Option[String],
    following: Boolean,
):
  def toHttpProfile: Profile =
    Profile(
      username = username,
      bio = bio,
      image = image,
      following = following
    )
  def toHttp: ProfileBody =
    ProfileBody(profile = toHttpProfile)

final case class UpdateArticle(
    slug: String,
    title: String,
    description: String,
    body: String,
)

object UpdateArticle:
  def fromHttp(
      req: entities.UpdateArticleReqBody,
      slug: Option[String],
      existingArticle: db.Article): Validated[UpdateArticle] =
    (
      req.article.title
        .map(newTitle => Validation.nonEmptyString("article.title")(newTitle))
        .getOrElse(existingArticle.title.validNec),
      req.article.description
        .map(newDescription => Validation.nonEmptyString("article.description")(newDescription))
        .getOrElse(existingArticle.description.validNec),
      req.article.body
        .map(newBody => Validation.nonEmptyString("article.body")(newBody))
        .getOrElse(existingArticle.body.validNec)
    ).mapN { (title, description, body) =>
      db.UpdateArticle(
        slug = slug.getOrElse(existingArticle.slug),
        title = title,
        description = description,
        body = body,
      )
    }

final case class UserCoordinates(username: String, id: UserId)

final case class ArticleQuery[T](
    tag: Option[String] = None,
    author: Option[String] = None,
    favoritedBy: Option[T] = None):
  def allEmpty: Boolean = tag.isEmpty && author.isEmpty && favoritedBy.isEmpty

final case class Pagination(
    offset: Int,
    limit: Int
)
object Pagination:
  private val DefaultOffset = 0
  private val DefaultLimit  = 20
  def fromReq(offset: Option[Int], limit: Option[Int]): Pagination =
    Pagination(offset = offset.getOrElse(DefaultOffset), limit = limit.getOrElse(DefaultLimit))
