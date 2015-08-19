package controllers

import jp.t2v.lab.play2.auth.AuthElement
import models.{User, NormalUser, Tweet}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
import play.api.mvc._
import play.api.i18n.{MessagesApi, I18nSupport}


import jp.co.bizreach.elasticsearch4s._

import javax.inject.Inject
import services.{ManageTweetService, ManageUserService}

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class RootController @Inject()(
                                val messagesApi: MessagesApi,
                                val manageUserService: ManageUserService,
                                val manageTweetService: ManageTweetService) extends Controller
with I18nSupport with AuthElement with AuthConfigImpl {

  /**
   * 一覧表示
   */
  def toLong: Any => Long = {
    case x: Integer => x.toLong
    case x: Long => x
  }

  def index = AsyncStack(AuthorityKey -> NormalUser) { implicit rs =>
    val user = loggedIn
    manageTweetService.getTweetsByUserIdList(user.follow.map(toLong)).flatMap { tweets =>
      manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>
        val tweetsWithUser = tweets.map { tweet =>
          (tweet, users.find(user => user.id == tweet.user_id))
        }.filter { case (tweet, user) => user.isDefined }
          .map { case (tweet, user) => (tweet, user.get) }

        Ok(views.html.index(tweetsWithUser))
      }
    }
  }

  def welcome = Action.async { implicit rs =>
    Future(Ok(views.html.welcome()))
  }

  def profile(screenName: String) = Action.async { implicit rs =>
    manageUserService.getUserByScreenName(screenName).flatMap{userOption =>
      manageTweetService.getTweetsByUserId(userOption.get.id).map { tweets =>
        Ok(views.html.profile(userOption.get, tweets.map((_, userOption.get))))
      }
    }
  }

  def favicon = TODO

  def follow(screenName: String) = Action.async { implicit rs =>
    manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
      manageUserService.getUsersByUserIdList(userOption.get.follow.map(toLong)).map { following =>
        Ok(views.html.follow(following))
      }
    }
  }
}
