package pl.msitko.realworld.endpoints

import cats.effect.IO
import io.circe.generic.auto.*
import io.circe.{Decoder, Encoder}
import pl.msitko.realworld.{db, JwtConfig}
import pl.msitko.realworld.Entities.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.PartialServerEndpoint

import java.util.UUID

class ArticleEndpoints(jwtConfig: JwtConfig) extends SecuredEndpoints(jwtConfig):
  val listArticles = endpoint.get
    .in("api" / "articles")
    .in(query[String]("tag"))
    .in(query[String]("author"))
    .in(query[String]("favorited"))
    .in(query[String]("limit"))
    .in(query[String]("offset"))
    .out(jsonBody[Articles])

  val feedArticles = endpoint.get
    .in("api" / "articles" / "feed")
    .in(query[String]("limit"))
    .in(query[String]("offset"))
    .out(jsonBody[Articles])

  val getArticle = endpoint.get
    .in("api" / "articles")
    .in(path[String])
    .out(jsonBody[ArticleBody])

  val createArticle =
    secureEndpoint.post
      .in("api" / "articles")
      .in(jsonBody[CreateArticleReqBody])
      .out(jsonBody[ArticleBody])
      .out(statusCode(StatusCode.Created))

  val updateArticle = endpoint.put
    .in("api" / "articles")
    .in(jsonBody[UpdateArticleReqBody])
    .out(jsonBody[ArticleBody])

  val deleteArticle = endpoint.delete
    .in("api" / "articles")
    .in(path[String])
    .out(jsonBody[Unit])

  val addComment = endpoint.post
    .in("api" / "articles")
    .in(path[String])
    .in("comments")
    .in(jsonBody[AddCommentReqBody])
    .out(jsonBody[CommentBody])

  val getComments = endpoint.get
    .in("api" / "articles")
    .in(path[String])
    .in("comments")
    .out(jsonBody[Comments])

  val deleteComment = endpoint.delete
    .in("api" / "articles")
    .in(path[String])
    .in("comments")
    .in(path[String])
    .out(jsonBody[Unit])

  val favoriteArticle = endpoint.post
    .in("api" / "articles")
    .in(path[String])
    .in("favorite")
    .out(jsonBody[ArticleBody])

  val unfavoriteArticle = endpoint.delete
    .in("api" / "articles")
    .in(path[String])
    .in("favorite")
    .out(jsonBody[ArticleBody])
