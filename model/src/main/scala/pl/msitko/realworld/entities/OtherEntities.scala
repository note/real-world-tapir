package pl.msitko.realworld.entities

final case class Profile(username: String, bio: Option[String], image: Option[String], following: Boolean):
  def body: ProfileBody = ProfileBody(profile = this)

final case class ProfileBody(profile: Profile)

final case class Tags(tags: List[String])
