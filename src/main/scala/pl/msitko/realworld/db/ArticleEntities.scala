package pl.msitko.realworld.db

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.implicits.legacy.instant.*

import java.time.Instant
import java.util.UUID

final case class ArticleNoId(
    slug: String,
    title: String,
    description: String,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
)

final case class Article(
    id: UUID,
    slug: String,
    title: String,
    description: String,
    body: String,
    createdAt: Instant,
    updatedAt: Instant,
    authorId: UUID,
)

// FullArticle represents a result row of data related to a single article and JOINed over many tables
final case class FullArticle(
    article: Article,
    author: Author,
    favoritesCount: Option[Int],
    favorited: Option[Int],
    tags: List[String]
)

object FullArticle:
  implicit val fullArticleRead: Read[FullArticle] =
    Read[(
        UUID,
        String,
        String,
        String,
        String,
        Instant,
        Instant,
        UUID,
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
)

final case class UpdateArticle(
    slug: String,
    title: String,
    description: String,
    body: String,
)

final case class UserCoordinates(username: String, id: UUID)

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
