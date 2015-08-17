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

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class RootController @Inject()(
                                val messagesApi: MessagesApi,
                                val userService: ManageUserService,
                                val tweetService: ManageTweetService) extends Controller
    with I18nSupport with AuthElement with AuthConfigImpl {

  /**
   * 一覧表示
   */
  def toLong: Any => Long = {
    case x: Integer  => x.toLong
    case x: Long => x
  }

  def index = StackAction(AuthorityKey -> NormalUser) { implicit rs =>
    val user = loggedIn
    val tweets = tweetService.getTweetsByUserIdList(user.follow.map(toLong))
    val tweetsWithUser = tweets.map { tweet =>
      (tweet, userService.getUserById(tweet.user_id).get)
    }
    Ok(views.html.index(tweetsWithUser))
  }

  def welcome = Action { implicit rs =>
    Ok(views.html.welcome())
  }

  def profile(screenName: String) = Action.async { implicit rs =>
    val user = userService.getUserByScreenName(screenName).get
    val tweets = tweetService.getTweetsByUserId(user.id)
    val tweetsWithUser = tweets.map((_, user))
    Future {
      Ok(views.html.profile(user, tweetsWithUser))
    }
  }

  def favicon = TODO

  def follow(screenName: String) = Action.async { implicit rs =>
    val user = userService.getUserByScreenName(screenName).get
    val following = userService.getUsersByUserIdList(user.follow.map(toLong))
    Future {
      Ok(views.html.follow(following))
    }
  }
}
