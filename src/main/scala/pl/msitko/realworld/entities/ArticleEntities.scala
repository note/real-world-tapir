package pl.msitko.realworld.entities

import cats.syntax.all.*
import pl.msitko.realworld.{db, Validated, Validation}

import java.time.Instant

final case class UpdateArticleReq(title: Option[String], description: Option[String], body: Option[String])

final case class CreateArticleReq(title: String, description: String, body: String, tagList: List[String])

final case class UpdateArticleReqBody(article: UpdateArticleReq):
  def toDB(slug: Option[String], existingArticle: db.Article): Validated[db.UpdateArticle] =
    (
      article.title
        .map(newTitle => Validation.nonEmptyString("article.title")(newTitle))
        .getOrElse(existingArticle.title.validNec),
      article.description
        .map(newDescription => Validation.nonEmptyString("article.description")(newDescription))
        .getOrElse(existingArticle.description.validNec),
      article.body
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
    Article(
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
  def toDB(slug: String, now: Instant): Validated[db.ArticleNoId] =
    (
      Validation.nonEmptyString("article.title")(article.title),
      Validation.nonEmptyString("article.description")(article.description),
      Validation.nonEmptyString("article.body")(article.body)).mapN { (title, description, body) =>
      db.ArticleNoId(
        slug = slug,
        title = title,
        description = description,
        body = body,
        tags = article.tagList,
        createdAt = now,
        updatedAt = now,
      )
    }

final case class Articles(articles: List[Article], articlesCount: Int)
object Articles:
  def fromArticles(articles: List[Article]): Articles = Articles(articles = articles, articlesCount = articles.size)
