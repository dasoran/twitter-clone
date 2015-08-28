package controllers

import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

import jp.t2v.lab.play2.auth.OptionalAuthElement
import models._
import models.forms.TweetForm
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{WebSocket, Controller}
import services._

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/**
 * Created by kyota.yasuda on 15/08/24.
 */
class ApiController @Inject()(
                               val messagesApi: MessagesApi,
                               val manageUserService: ManageUserService,
                               val manageTweetService: ManageTweetService,
                               val userService: UserService,
                               val graphService: GraphService,
                               val manageGroupService: ManageGroupService) extends Controller
with I18nSupport with OptionalAuthElement with AuthConfigImpl {


  case class TweetWithUser(tweet: TweetForJson,
                           user: models.User,
                           myId: Long)

  case class TweetForJson(id: String,
                          user_id: Long,
                          text: String,
                          created_at: String,
                          favorited_user_id: List[Long])

  def timelineUpdate(lastId: Long) = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getTweetsByUserIdListToTheTweet(user.id :: user.follow, 20, lastId).flatMap { tweets =>
          manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>

            val tweetsWithUser: List[TweetWithUser] = tweets.map { tweet =>
              TweetForJson(tweet.id.toString,
                tweet.user_id,
                tweet.text,
                tweet.created_at.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                tweet.favorited_user_id)
            }.map { tweet =>
              (tweet, users.find(user => user.id == tweet.user_id))
            }.filter { case (tweet, user) => user.isDefined }
              .map { case (tweet, _user) => TweetWithUser(tweet, _user.get, user.id) }

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

  def timeline = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getTweetsByUserIdList(user.id :: user.follow, 20).flatMap { tweets =>
          manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>

            val tweetsWithUser: List[TweetWithUser] = tweets.map { tweet =>
              TweetForJson(tweet.id.toString,
                tweet.user_id,
                tweet.text,
                tweet.created_at.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                tweet.favorited_user_id)
            }.map { tweet =>
              (tweet, users.find(user => user.id == tweet.user_id))
            }.filter { case (tweet, user) => user.isDefined }
              .map { case (tweet, _user) => TweetWithUser(tweet, _user.get, user.id) }

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


  def groupTimelineUpdate(groupId: Long, lastId: Long) = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageGroupService.getGroupById(groupId).flatMap{ group =>
          manageTweetService.getTweetsByUserIdListToTheTweet(group.get.users.toList, 20, lastId).flatMap { tweets =>
            manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>

              val tweetsWithUser: List[TweetWithUser] = tweets.map { tweet =>
                TweetForJson(tweet.id.toString,
                  tweet.user_id,
                  tweet.text,
                  tweet.created_at.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                  tweet.favorited_user_id)
              }.map { tweet =>
                (tweet, users.find(user => user.id == tweet.user_id))
              }.filter { case (tweet, user) => user.isDefined }
                .map { case (tweet, _user) => TweetWithUser(tweet, _user.get, user.id) }

              implicit val tweetWrites = Json.writes[TweetForJson]
              implicit val userWrites = Json.writes[models.User]
              implicit val tweetWithUserWrites = Json.writes[TweetWithUser]

              Ok(Json.toJson(tweetsWithUser))
            }
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def groupTimeline(groupId: Long) = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageGroupService.getGroupById(groupId).flatMap{ group =>
          manageTweetService.getTweetsByUserIdList(group.get.users.toList, 20).flatMap { tweets =>
            manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>

              val tweetsWithUser: List[TweetWithUser] = tweets.map { tweet =>
                TweetForJson(tweet.id.toString,
                  tweet.user_id,
                  tweet.text,
                  tweet.created_at.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                  tweet.favorited_user_id)
              }.map { tweet =>
                (tweet, users.find(user => user.id == tweet.user_id))
              }.filter { case (tweet, user) => user.isDefined }
                .map { case (tweet, _user) => TweetWithUser(tweet, _user.get, user.id) }

              implicit val tweetWrites = Json.writes[TweetForJson]
              implicit val userWrites = Json.writes[models.User]
              implicit val tweetWithUserWrites = Json.writes[TweetWithUser]

              Ok(Json.toJson(tweetsWithUser))
            }
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def getIndex(groupId: Long) = AsyncStack {implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageGroupService.getGroupById(groupId).flatMap { group =>
          graphService.createIndex(group.get).map { indexes =>
            Ok(Json.toJson(indexes.take(5)))
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def replyUpdate(lastId: Long) = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) =>
        manageTweetService.getReplyTweetsByUserIdListToTheTweet(user.id :: user.follow, user.screen_name, 20, lastId).flatMap { tweets =>
          manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>

            val tweetsWithUser: List[TweetWithUser] = tweets.map { tweet =>
              TweetForJson(tweet.id.toString,
                tweet.user_id,
                tweet.text,
                tweet.created_at.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                tweet.favorited_user_id)
            }.map { tweet =>
              (tweet, users.find(user => user.id == tweet.user_id))
            }.filter { case (tweet, user) => user.isDefined }
              .map { case (tweet, _user) => TweetWithUser(tweet, _user.get, user.id) }

            implicit val tweetWrites = Json.writes[TweetForJson]
            implicit val userWrites = Json.writes[models.User]
            implicit val tweetWithUserWrites = Json.writes[TweetWithUser]

            Ok(Json.toJson(tweetsWithUser))
          }
        }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def reply = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getReplyTweetsByUserIdList(user.id :: user.follow, user.screen_name, 20).flatMap { tweets =>
          manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>

            val tweetsWithUser: List[TweetWithUser] = tweets.map { tweet =>
              TweetForJson(tweet.id.toString,
                tweet.user_id,
                tweet.text,
                tweet.created_at.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                tweet.favorited_user_id)
            }.map { tweet =>
              (tweet, users.find(user => user.id == tweet.user_id))
            }.filter { case (tweet, user) => user.isDefined }
              .map { case (tweet, _user) => TweetWithUser(tweet, _user.get, user.id) }

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


  val tweetForm = Form(
    mapping(
      "tweetInput" -> nonEmptyText(maxLength = 140)
    )(TweetForm.apply)(TweetForm.unapply)
  )

  case class APIResponse(code: Int,
                         message: String)


  def postTweet = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        tweetForm.bindFromRequest.fold(
          // エラーの場合
          error => {
            //BadRequest(views.html.signup(error, signupForm))
            println(error.errorsAsJson)
            Future {
              implicit val apiResponseWrites = Json.writes[APIResponse]
              val apiResponse = APIResponse(
                code = 500,
                message = "tweet failed"
              )
              BadRequest(Json.toJson(apiResponse))
            }
          },
          // OKの場合
          form => {
            val r = new Random(new SecureRandom())
            val tweet = Tweet(
              id = Math.abs(r.nextLong()),
              user_id = user.id,
              text = form.tweetInput,
              created_at = LocalDateTime.now().plusHours(-9),
              favorited_user_id = List()
            )
            manageTweetService.insertTweet(tweet).map { f =>
              Thread.sleep(1000)
              implicit val apiResponseWrites = Json.writes[APIResponse]
              val apiResponse = APIResponse(
                code = 200,
                message = "successful"
              )
              Ok(Json.toJson(apiResponse))
            }
          }
        )
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def addFavorite(tweetId: Long) = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getTweetById(tweetId).flatMap{ tweet =>
          if (tweet.get.favorited_user_id.contains(user.id)) {
            Future {
              implicit val apiResponseWrites = Json.writes[APIResponse]
              val apiResponse = APIResponse(
                code = 500,
                message = "already favorited"
              )
              Ok(Json.toJson(apiResponse))
            }
          } else {
            val toUpdate = tweet.get.copy(favorited_user_id = user.id :: tweet.get.favorited_user_id)
            manageTweetService.updateTweet(toUpdate).map{ f =>
              implicit val apiResponseWrites = Json.writes[APIResponse]
              val apiResponse = APIResponse(
                code = 200,
                message = "successful"
              )
              Ok(Json.toJson(apiResponse))
            }
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def removeFavorite(tweetId: Long) = AsyncStack { implicit rs =>

    loggedIn match {
      case Some(user) => {
        manageTweetService.getTweetById(tweetId).flatMap{ tweet =>
          if (!tweet.get.favorited_user_id.contains(user.id)) {
            Future {
              implicit val apiResponseWrites = Json.writes[APIResponse]
              val apiResponse = APIResponse(
                code = 500,
                message = "already unfavorited"
              )
              Ok(Json.toJson(apiResponse))
            }
          } else {
            val toUpdate = tweet.get.copy(favorited_user_id = tweet.get.favorited_user_id.filter(_ != user.id))
            manageTweetService.updateTweet(toUpdate).map{ f =>
              implicit val apiResponseWrites = Json.writes[APIResponse]
              val apiResponse = APIResponse(
                code = 200,
                message = "successful"
              )
              Ok(Json.toJson(apiResponse))
            }
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def upload = AsyncStack(parse.multipartFormData) { request =>
    Future {
      val r = new Random(new SecureRandom())
      request.body.file("picture").map { picture =>
        import java.io.File
        val filename = Math.abs(r.nextLong())
        val contentType = picture.contentType
        picture.ref.moveTo(new File(s"./public/img/uploaded/$filename"))
        Thread.sleep(2000)
        implicit val apiResponseWrites = Json.writes[APIResponse]
        val apiResponse = APIResponse(
          code = 200,
          message = "img/uploaded/" + filename
        )
        Ok(Json.toJson(apiResponse))
      }.getOrElse {
        implicit val apiResponseWrites = Json.writes[APIResponse]
        val apiResponse = APIResponse(
          code = 500,
          message = "upload failed"
        )
        Ok(Json.toJson(apiResponse))
      }
    }
  }

  def delete(tweetId: Long) = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getTweetById(tweetId).flatMap {
          case Some(x) => {
            x match {
              case x if x.user_id == user.id => {
                manageTweetService.deleteTweet(tweetId).map { f=>
                  implicit val apiResponseWrites = Json.writes[APIResponse]
                  val apiResponse = APIResponse(
                    code = 200,
                    message = "sccessful"
                  )
                  Ok(Json.toJson(apiResponse))
                }
              }
              case _ => {
                implicit val apiResponseWrites = Json.writes[APIResponse]
                val apiResponse = APIResponse(
                  code = 500,
                  message = "The tweet is not your's tweet"
                )
                Future(Ok(Json.toJson(apiResponse)))
              }
            }
          }
          case None => {
            implicit val apiResponseWrites = Json.writes[APIResponse]
            val apiResponse = APIResponse(
              code = 500,
              message = "tweet not exist"
            )
            Future(Ok(Json.toJson(apiResponse)))
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

}
