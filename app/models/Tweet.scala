package models

import java.time.LocalDateTime

/**
 * Created by kyota.yasuda on 15/08/13.
 */
case class Tweet(id: Long,
                 user_id: Long,
                 text: String,
                 created_at: LocalDateTime,
                 favorited_user_id: List[Long])
