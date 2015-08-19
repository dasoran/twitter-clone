package models

/**
 * Created by kyota.yasuda on 15/08/19.
 */
case class TweetDB(id: Long,
                   user_id: Long,
                   text: String,
                   created_at: String,
                   retweet_count: Integer,
                   favorite_count: Integer)
