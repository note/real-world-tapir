package pl.msitko.realworld

import pl.msitko.realworld.db.{ArticleId, UserId}

import java.time.Instant

object Entities:
  final case class AuthenticationReqBodyUser(email: String, password: String)

  final case class AuthenticationReqBody(user: AuthenticationReqBodyUser)

  final case class User(
      email: String,
      token: String,
      username: String,
      bio: Option[String],
      image: Option[String],
  ):
    def body: UserBody = UserBody(user = this)

  final case class AddCommentReq(body: String)

  final case class AddCommentReqBody(comment: AddCommentReq):
    def toDB(authorId: UserId, articleId: ArticleId, now: Instant): db.CommentNoId =
      db.CommentNoId(authorId = authorId, articleId = articleId, body = comment.body, createdAt = now, updatedAt = now)

  final case class Comment(id: Int, createdAt: Instant, updatedAt: Instant, body: String, author: Profile):
    def toBody: CommentBody = CommentBody(comment = this)
  object Comment:
    def fromDB(dbComment: db.FullComment): Comment =
      Comment(
        id = dbComment.comment.id,
        createdAt = dbComment.comment.createdAt,
        updatedAt = dbComment.comment.createdAt,
        body = dbComment.comment.body,
        author = Profile.fromDB(dbComment.author))

  final case class CommentBody(comment: Comment)
  object CommentBody:
    def fromDB(dbComment: db.FullComment): CommentBody =
      CommentBody(comment = Comment.fromDB(dbComment))

  final case class Comments(comments: List[Comment])

  final case class UserBody(user: User)
  object UserBody:
    def fromDB(dbUser: db.User, jwtToken: String): UserBody =
      UserBody(user = Entities.User(
        email = dbUser.email,
        token = jwtToken,
        username = dbUser.username,
        bio = dbUser.bio,
        image = dbUser.image,
      ))

  final case class RegistrationUserBody(
      username: String,
      email: String,
      password: String,
      bio: Option[String],
      image: Option[String])

  final case class RegistrationReqBody(user: RegistrationUserBody)

  final case class Profile(username: String, bio: Option[String], image: Option[String], following: Boolean):
    def body: ProfileBody = ProfileBody(profile = this)

  object Profile:
    def fromDB(author: db.Author) =
      Profile(
        username = author.username,
        bio = author.bio,
        image = author.image,
        following = author.following
      )

  final case class ProfileBody(profile: Profile)

  object ProfileBody:
    def fromDB(author: db.Author): ProfileBody = ProfileBody(Profile.fromDB(author))

  final case class UpdateUserBody(
      email: Option[String],
      username: Option[String],
      password: Option[String],
      image: Option[String],
      bio: Option[String],
  )

  final case class UpdateUserReqBody(user: UpdateUserBody):
    def toDB(existingUser: db.User): db.UpdateUser =
      db.UpdateUser(
        email = user.email.getOrElse(existingUser.email),
        username = user.username.getOrElse(existingUser.username),
        password = user.password,
        // TODO: Current treatment of bio and image is problematic in case of nullifying those values as part of update
        // On the other hand specs don't tell anything explicitly about nullifying. I guess something like following
        // would make sense:
        // Omitting value in update request means "no change"
        // Specifying value to be null explicitly means "change the value to null"
        bio = user.bio.orElse(existingUser.bio),
        image = user.image.orElse(existingUser.image)
      )

  final case class UpdateArticleReq(title: Option[String], description: Option[String], body: Option[String])

  final case class UpdateArticleReqBody(article: UpdateArticleReq):
    def toDB(slug: Option[String], existingArticle: db.Article): db.UpdateArticle =
      db.UpdateArticle(
        slug = slug.getOrElse(existingArticle.slug),
        title = article.title.getOrElse(existingArticle.title),
        description = article.description.getOrElse(existingArticle.description),
        body = article.body.getOrElse(existingArticle.body),
      )

  final case class Tags(tags: List[String])
  object Tags:
    def fromDB(tags: List[String]): Tags = Tags(tags)

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
      Entities.Article(
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

  final case class CreateArticleReq(title: String, description: String, body: String, tagList: List[String])

  final case class CreateArticleReqBody(article: CreateArticleReq):
    def toDB(slug: String, now: Instant): db.ArticleNoId =
      db.ArticleNoId(
        slug = slug,
        title = article.title,
        description = article.description,
        body = article.body,
        tags = article.tagList,
        createdAt = now,
        updatedAt = now,
      )

  final case class Articles(articles: List[Article], articlesCount: Int)
  object Articles:
    def fromArticles(articles: List[Article]): Articles = Articles(articles = articles, articlesCount = articles.size)
