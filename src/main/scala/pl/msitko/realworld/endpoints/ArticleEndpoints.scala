package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import pl.msitko.realworld.JwtConfig
import pl.msitko.realworld.Entities.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

class ArticleEndpoints(jwtConfig: JwtConfig) extends SecuredEndpoints(jwtConfig):
  val listArticles = endpoint.get
    .in("api" / "articles")
    .in(query[String]("tag"))
    .in(query[String]("author"))
    .in(query[String]("favorited"))
    .in(query[String]("limit"))
    .in(query[String]("offset"))
    .out(jsonBody[Articles])

  val feedArticles = secureEndpoint.get
    .in("api" / "articles" / "feed")
    .in(query[String]("limit"))
    .in(query[String]("offset"))
    .out(jsonBody[Articles])

  val getArticle = optionallySecureEndpoint.get
    .in("api" / "articles")
    .in(path[String])
    .out(jsonBody[ArticleBody])

  val createArticle =
    secureEndpoint.post
      .in("api" / "articles")
      .in(jsonBody[CreateArticleReqBody])
      .out(jsonBody[ArticleBody])
      .out(statusCode(StatusCode.Created))

  val updateArticle = secureEndpoint.put
    .in("api" / "articles")
    .in(path[String])
    .in(jsonBody[UpdateArticleReqBody])
    .out(jsonBody[ArticleBody])

  val deleteArticle = secureEndpoint.delete
    .in("api" / "articles")
    .in(path[String])
    .out(jsonBody[Unit])

  val addComment = secureEndpoint.post
    .in("api" / "articles")
    .in(path[String])
    .in("comments")
    .in(jsonBody[AddCommentReqBody])
    .out(jsonBody[CommentBody])

  val getComments = optionallySecureEndpoint.get
    .in("api" / "articles")
    .in(path[String])
    .in("comments")
    .out(jsonBody[Comments])

  val deleteComment = secureEndpoint.delete
    .in("api" / "articles")
    .in(path[String])
    .in("comments")
    .in(path[Int])
    .out(jsonBody[Unit])

  val favoriteArticle = secureEndpoint.post
    .in("api" / "articles")
    .in(path[String])
    .in("favorite")
    .out(jsonBody[ArticleBody])

  val unfavoriteArticle = secureEndpoint.delete
    .in("api" / "articles")
    .in(path[String])
    .in("favorite")
    .out(jsonBody[ArticleBody])
