package pl.msitko.realworld.endpoints

import io.circe.generic.auto.*
import pl.msitko.realworld.entities.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object ArticleEndpoints:
  val listArticles = SecuredEndpoints.optionallySecureEndpoint.get
    .in("api" / "articles")
    .in(query[Option[String]]("tag"))
    .in(query[Option[String]]("author"))
    .in(query[Option[String]]("favorited"))
    .in(query[Option[Int]]("limit"))
    .in(query[Option[Int]]("offset"))
    .out(jsonBody[Articles])
    .tag("articles")

  val feedArticles = SecuredEndpoints.secureEndpoint.get
    .in("api" / "articles" / "feed")
    .in(query[Option[Int]]("limit"))
    .in(query[Option[Int]]("offset"))
    .out(jsonBody[Articles])
    .tag("articles")

  val getArticle = SecuredEndpoints.optionallySecureEndpoint.get
    .in("api" / "articles")
    .in(path[String].name("slug").description("slug of the article"))
    .out(jsonBody[ArticleBody])
    .tag("articles")

  val createArticle =
    SecuredEndpoints.secureEndpoint.post
      .in("api" / "articles")
      .in(jsonBody[CreateArticleReqBody])
      .out(jsonBody[ArticleBody])
      .out(statusCode(StatusCode.Created))
      .tag("articles")

  val updateArticle = SecuredEndpoints.secureEndpoint.put
    .in("api" / "articles")
    .in(path[String].name("slug").description("slug of the article being edited"))
    .in(jsonBody[UpdateArticleReqBody])
    .out(jsonBody[ArticleBody])
    .tag("articles")

  val deleteArticle = SecuredEndpoints.secureEndpoint.delete
    .in("api" / "articles")
    .in(path[String].name("slug").description("slug of the article being edited"))
    .out(jsonBody[Unit])
    .tag("articles")

  val addComment = SecuredEndpoints.secureEndpoint.post
    .in("api" / "articles")
    .in(path[String].name("slug").description("slug of the article"))
    .in("comments")
    .in(jsonBody[AddCommentReqBody])
    .out(statusCode(StatusCode.Created))
    .out(jsonBody[CommentBody])
    .tag("comments")

  val getComments = SecuredEndpoints.optionallySecureEndpoint.get
    .in("api" / "articles")
    .in(path[String].name("slug").description("slug of the article"))
    .in("comments")
    .out(jsonBody[Comments])
    .tag("comments")

  val deleteComment = SecuredEndpoints.secureEndpoint.delete
    .in("api" / "articles")
    .in(path[String].name("slug").description("slug of the article"))
    .in("comments")
    .in(path[Int].name("commentId").description("id of the comment"))
    .out(jsonBody[Unit])
    .tag("comments")

  val favoriteArticle = SecuredEndpoints.secureEndpoint.post
    .in("api" / "articles")
    .in(path[String].name("slug").description("slug of the article"))
    .in("favorite")
    .out(jsonBody[ArticleBody])
    .out(statusCode(StatusCode.Created))
    .tag("articles")

  val unfavoriteArticle = SecuredEndpoints.secureEndpoint.delete
    .in("api" / "articles")
    .in(path[String].name("slug").description("slug of the article"))
    .in("favorite")
    .out(jsonBody[ArticleBody])
    .tag("articles")
