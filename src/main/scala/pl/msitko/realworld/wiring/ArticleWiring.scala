package pl.msitko.realworld.wiring

import cats.effect.IO
import pl.msitko.realworld.Entities
import pl.msitko.realworld.db.Pagination
import pl.msitko.realworld.endpoints.ArticleEndpoints
import pl.msitko.realworld.services.ArticleService
import sttp.tapir.server.ServerEndpoint

object ArticleWiring:
  def enpoints(articleEndpoints: ArticleEndpoints, service: ArticleService): List[ServerEndpoint[Any, IO]] =
    List(
      articleEndpoints.listArticles.serverLogicSuccess(_ => IO.pure(Entities.Articles(articles = List.empty))),
      articleEndpoints.feedArticles.serverLogicSuccess(userId =>
        (limit, offset) => service.feedArticles(userId, Pagination(limit = limit, offset = offset))),
      articleEndpoints.getArticle.serverLogic(service.getArticle),
      articleEndpoints.createArticle.serverLogic(service.createArticle),
      articleEndpoints.updateArticle.serverLogic(service.updateArticle),
      articleEndpoints.deleteArticle.serverLogic(service.deleteArticle),
      articleEndpoints.addComment.serverLogic(service.addComment),
      articleEndpoints.getComments.serverLogic(service.getComments),
      articleEndpoints.deleteComment.serverLogic(userId => (_, commentId) => service.deleteComment(userId)(commentId)),
      articleEndpoints.favoriteArticle.serverLogic(service.favoriteArticle),
      articleEndpoints.unfavoriteArticle.serverLogic(service.unfavoriteArticle)
    )
