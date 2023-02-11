package pl.msitko.realworld.db

import doobie.Meta

import java.time.Instant
import java.util.UUID

//trait Repo:
//  implicit protected val uuidMeta: Meta[UUID] = Meta.StringMeta.imap(UUID.fromString)(_.toString)
//
//  Meta.Advanced.other[UUID]("uuid")
