package controllers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import controllers.AuthConfigImpl
import jp.t2v.lab.play2.auth.OptionalAuthElement
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{WebSocket, Controller}
import services.{GraphService, UserService, ManageTweetService, ManageUserService}

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by kyota.yasuda on 15/08/24.
 */
class ApiController @Inject()(val manageUserService: ManageUserService,
                              val manageTweetService: ManageTweetService,
                              val userService: UserService,
                              val graphService: GraphService) extends Controller
with OptionalAuthElement with AuthConfigImpl {

  case class TweetWithUser(tweet: TweetForJson,
                           user: models.User)

  case class TweetForJson(id: Long,
                          user_id: Long,
                          text: String,
                          created_at: String,
                          retweet_count: Int,
                          favorite_count: Int)

  def timeline = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getTweetsByUserIdList(user.follow, 20).flatMap { tweets =>
          manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>

            val tweetsWithUser: List[TweetWithUser] = tweets.map { tweet =>
              TweetForJson(tweet.id,
                tweet.user_id,
                tweet.text,
                tweet.created_at.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                tweet.retweet_count,
                tweet.favorite_count)
            }.map { tweet =>
              (tweet, users.find(user => user.id == tweet.user_id))
            }.filter { case (tweet, user) => user.isDefined }
              .map { case (tweet, user) => TweetWithUser(tweet, user.get) }

            implicit val tweetWrites = Json.writes[TweetForJson]
            implicit val userWrites = Json.writes[models.User]
            implicit val tweetWithUserWrites = Json.writes[TweetWithUser]

            Ok(Json.toJson(tweetsWithUser))
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def reply = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getReplyTweetsByUserIdList(user.follow, user.screen_name, 20).flatMap { tweets =>
          manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>

            val tweetsWithUser: List[TweetWithUser] = tweets.map { tweet =>
              TweetForJson(tweet.id,
                tweet.user_id,
                tweet.text,
                tweet.created_at.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                tweet.retweet_count,
                tweet.favorite_count)
            }.map { tweet =>
              (tweet, users.find(user => user.id == tweet.user_id))
            }.filter { case (tweet, user) => user.isDefined }
              .map { case (tweet, user) => TweetWithUser(tweet, user.get) }

            implicit val tweetWrites = Json.writes[TweetForJson]
            implicit val userWrites = Json.writes[models.User]
            implicit val tweetWithUserWrites = Json.writes[TweetWithUser]

            Ok(Json.toJson(tweetsWithUser))
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }
}
