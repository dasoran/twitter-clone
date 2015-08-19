package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{ManageTweetService, ManageUserService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DebugController @Inject()(
                                 val messagesApi: MessagesApi,
                                 val manageUserService: ManageUserService,
                                 val manageTweetService: ManageTweetService)
  extends Controller with I18nSupport {

  /**
   * 一覧表示
   */
  def toLong: Any => Long = {
    case x: Integer => x.toLong
    case x: Long => x
  }

  def tweetList = Action.async { implicit rs =>
    manageTweetService.getTweets.flatMap { tweets =>
      manageUserService.getUsersByUserIdList(tweets.map(_.user_id))
        .map { users =>
          val tweetsWithUser = tweets.map { tweet =>
            (tweet, users.find(user => user.id == tweet.user_id))
          }.filter { case (tweet, user) => user.isDefined }
            .map { case (tweet, user) => (tweet, user.get) }
          Ok(views.html.debug.tweetlist(tweetsWithUser))
        }
    }
  }

  def userList = Action.async { implicit rs =>
    manageUserService.getUsers
      .map { users =>
        Ok(views.html.debug.userlist(users))
      }
  }
}
