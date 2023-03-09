package pl.msitko.realworld.entities

import cats.data.ValidatedNec
import cats.syntax.all._
import pl.msitko.realworld.db

import java.time.Instant

final case class UpdateArticleReq(title: Option[String], description: Option[String], body: Option[String])

final case class CreateArticleReq(title: String, description: String, body: String, tagList: List[String])

final case class UpdateArticleReqBody(article: UpdateArticleReq):
  def toDB(slug: Option[String], existingArticle: db.Article): db.UpdateArticle =
    db.UpdateArticle(
      slug = slug.getOrElse(existingArticle.slug),
      title = article.title.getOrElse(existingArticle.title),
      description = article.description.getOrElse(existingArticle.description),
      body = article.body.getOrElse(existingArticle.body),
    )
    

final case class ArticleBody(article: Article)
object ArticleBody:
  def fromDB(dbArticle: db.FullArticle): ArticleBody =
    ArticleBody(article = Article.fromDB(dbArticle))
    

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
object Article:
  def fromDB(dbArticle: db.FullArticle): Article =
    OtherEntities.Article(
      slug = dbArticle.article.slug,
      title = dbArticle.article.title,
      description = dbArticle.article.description,
      body = dbArticle.article.body,
      tagList = dbArticle.tags,
      createdAt = dbArticle.article.createdAt,
      updatedAt = dbArticle.article.updatedAt,
      favorited = dbArticle.favorited.isDefined,
      favoritesCount = dbArticle.favoritesCount.getOrElse(0),
      author = Profile.fromDB(dbArticle.author)
    )


final case class CreateArticleReqBody(article: CreateArticleReq):
  def toDB(slug: String, now: Instant): Validated[ArticleNoId] =
    if (article.body.isEmpty)
      ("body" -> "can't be empty").invalidNec
    else
      db.ArticleNoId(
        slug = slug,
        title = article.title,
        description = article.description,
        body = article.body,
        tags = article.tagList,
        createdAt = now,
        updatedAt = now,
      ).validNec
      

final case class Articles(articles: List[Article], articlesCount: Int)
object Articles:
  def fromArticles(articles: List[Article]): Articles = Articles(articles = articles, articlesCount = articles.size)
