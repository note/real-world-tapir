package pl.msitko.realworld.services

import cats.effect.IO
import pl.msitko.realworld.{Entities, ExampleResponses}
import pl.msitko.realworld.endpoints.ArticleEndpoints

object ArticleServices:
  val listArticlesImpl =
    ArticleEndpoints.listArticles.serverLogicSuccess(_ => IO.pure(Entities.Articles(articles = List.empty)))

  val feedArticlesImpl =
    ArticleEndpoints.feedArticles.serverLogicSuccess(_ => IO.pure(Entities.Articles(articles = List.empty)))

  val getArticleImpl =
    ArticleEndpoints.getArticle.serverLogicSuccess(slug => IO.pure(ExampleResponses.articleBody))

  val createArticleImpl =
    ArticleEndpoints.createArticle.serverLogicSuccess(_ => IO.pure(ExampleResponses.articleBody))

  val updateArticleImpl =
    ArticleEndpoints.updateArticle.serverLogicSuccess(_ => IO.pure(ExampleResponses.articleBody))

  val deleteArticleImpl =
    ArticleEndpoints.deleteArticle.serverLogicSuccess(_ => IO.pure(()))

  val addCommentImpl =
    ArticleEndpoints.addComment.serverLogicSuccess(slug => IO.pure(ExampleResponses.commentBody))

  val getCommentsImpl =
    ArticleEndpoints.getComments.serverLogicSuccess(slug =>
      IO.pure(Entities.Comments(comments = List(ExampleResponses.comment))))

  val deleteCommentImpl =
    ArticleEndpoints.deleteComment.serverLogicSuccess((slug, commentId) => IO.pure(()))

  val favoriteArticleImpl =
    ArticleEndpoints.favoriteArticle.serverLogicSuccess(slug => IO.pure(ExampleResponses.articleBody))

  val unfavoriteArticleImpl =
    ArticleEndpoints.unfavoriteArticle.serverLogicSuccess(slug => IO.pure(ExampleResponses.articleBody))

  def services = List(
    listArticlesImpl,
    feedArticlesImpl,
    getArticleImpl,
    createArticleImpl,
    updateArticleImpl,
    deleteArticleImpl,
    addCommentImpl,
    getCommentsImpl,
    deleteCommentImpl,
    favoriteArticleImpl,
    unfavoriteArticleImpl,
  )
