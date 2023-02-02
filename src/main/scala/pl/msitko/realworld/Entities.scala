package pl.msitko.realworld

import java.time.Instant

object Entities:
  final case class AuthenticationReqBodyUser(email: String, password: String)

  final case class AuthenticationReqBody(user: AuthenticationReqBodyUser)

  final case class User(
      email: String,
      token: String,
      username: String,
      bio: String,
      image: Option[String],
  ):
    def body: UserBody = UserBody(user = this)

  final case class AddCommentReq(body: String)

  final case class AddCommentReqBody(comment: AddCommentReq)

  final case class Comment(id: Int, createdAt: Instant, updatedAt: Instant, body: String, author: Profile):
    def toBody: CommentBody = CommentBody(comment = this)

  final case class CommentBody(comment: Comment)

  final case class Comments(comments: List[Comment])

  final case class UserBody(user: User)

  final case class RegistrationUserBody(username: String, email: String, password: String)

  final case class RegistrationReqBody(user: RegistrationUserBody)

  final case class Profile(username: String, bio: String, image: Option[String], following: Boolean):
    def body: ProfileBody = ProfileBody(profile = this)

  final case class ProfileBody(profile: Profile)

  final case class UpdateUserBody(
      email: Option[String],
      username: Option[String],
      password: Option[String],
      image: Option[String],
      bio: Option[String],
  )

  final case class UpdateUserReqBody(user: UpdateUserBody)

  final case class UpdateArticleReq(title: Option[String], description: Option[String], body: Option[String])

  final case class UpdateArticleReqBody(article: UpdateArticleReq)

  final case class Tags(tags: List[String])

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

  final case class CreateArticleReq(title: String, description: String, body: String, tagList: List[String])

  final case class CreateArticleReqBody(article: CreateArticleReq)

  // TODO: does is use circe?
  //  given instantEncoder: Encoder[Instant] = ???
  final case class Articles(articles: List[Article])
