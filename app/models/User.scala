package models

/**
 * Created by kyota.yasuda on 15/08/13.
 */
case class User(id: Long,
                password: Option[String] = None,
                screen_name: String,
                name: String,
                description: String,
                profile_image_url: String,
                follow: List[Long],
                follower: List[Long],
                email: Option[String] = None)
