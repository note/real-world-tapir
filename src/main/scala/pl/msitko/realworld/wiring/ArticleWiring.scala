package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.db.{ArticleQuery, Pagination}
import pl.msitko.realworld.endpoints.ArticleEndpoints
import pl.msitko.realworld.services.ArticleService
import sttp.tapir.server.ServerEndpoint

object ArticleWiring:
  def enpoints(articleEndpoints: ArticleEndpoints, service: ArticleService): List[ServerEndpoint[Any, IO]] =
    List(
      articleEndpoints.listArticles.serverLogicSuccess { userId => (tag, author, favoritedBy, limit, offset) =>
        val pagination = Pagination.fromReq(limit = limit, offset = offset)
        val query      = ArticleQuery[String](tag = tag, author = author, favoritedBy = favoritedBy)
        service.listArticles(userId, query, pagination)
      },
      articleEndpoints.feedArticles.serverLogicSuccess(userId =>
        (limit, offset) => service.feedArticles(userId, Pagination.fromReq(limit = limit, offset = offset))),
      articleEndpoints.getArticle.resultLogic(service.getArticle),
      articleEndpoints.createArticle.resultLogic(service.createArticle),
      articleEndpoints.updateArticle.resultLogic(service.updateArticle),
      articleEndpoints.deleteArticle.resultLogic(service.deleteArticle),
      articleEndpoints.addComment.resultLogic(service.addComment),
      articleEndpoints.getComments.resultLogic(service.getComments),
      articleEndpoints.deleteComment.resultLogic(userId => (_, commentId) => service.deleteComment(userId)(commentId)),
      articleEndpoints.favoriteArticle.resultLogic(service.favoriteArticle),
      articleEndpoints.unfavoriteArticle.resultLogic(service.unfavoriteArticle)
    )
