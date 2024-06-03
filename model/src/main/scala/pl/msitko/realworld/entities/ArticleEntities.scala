package pl.msitko.realworld.entities

import cats.syntax.all.*

import java.time.Instant

final case class UpdateArticleReq(title: Option[String], description: Option[String], body: Option[String])

final case class CreateArticleReq(title: String, description: String, body: String, tagList: List[String])

final case class UpdateArticleReqBody(article: UpdateArticleReq)

final case class ArticleBody(article: Article)

final case class Article(
    slug: String,
    title: String,
    description: String,
    body: String,
    tagList: List[String],
    createdAt: Instant,
    updatedAt: Instant,
    favorited: Boolean,
    favoritesCount: Int,
    author: Profile,
):
  def toBody: ArticleBody = ArticleBody(article = this)

final case class CreateArticleReqBody(article: CreateArticleReq)

final case class Articles(articles: List[Article], articlesCount: Int)
object Articles:
  def fromArticles(articles: List[Article]): Articles = Articles(articles = articles, articlesCount = articles.size)
