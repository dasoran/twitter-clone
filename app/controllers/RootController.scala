package controllers

import jp.t2v.lab.play2.auth.{OptionalAuthElement, AuthElement}
import models.NormalUser
import play.api.mvc.Results._
import play.api.mvc._
import play.api.i18n.{MessagesApi, I18nSupport}



import javax.inject.Inject
import services.{UserService, ManageTweetService, ManageUserService}

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class RootController @Inject()(
                                val messagesApi: MessagesApi,
                                val manageUserService: ManageUserService,
                                val manageTweetService: ManageTweetService,
                                val userService: UserService) extends Controller
with I18nSupport with OptionalAuthElement with AuthConfigImpl {


  def index = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getTweetsByUserIdList(user.follow).flatMap { tweets =>
          manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).map { users =>
            val tweetsWithUser = tweets.map { tweet =>
              (tweet, users.find(user => user.id == tweet.user_id))
            }.filter { case (tweet, user) => user.isDefined }
              .map { case (tweet, user) => (tweet, user.get) }

            Ok(views.html.index(tweetsWithUser))
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.welcome))
    }
  }

  def welcome = Action.async { implicit rs =>
    Future(Ok(views.html.welcome()))
  }

  def profile(screenName: String) = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
          manageTweetService.getTweetsByUserId(userOption.get.id).map { tweets =>
            Ok(views.html.profilewithlogin(user, userOption.get, tweets.map((_, userOption.get))))
          }
        }
      }
      case None => {
        manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
          manageTweetService.getTweetsByUserId(userOption.get.id).map { tweets =>
            Ok(views.html.profile(userOption.get, tweets.map((_, userOption.get))))
          }
        }
      }
    }
  }

  def favicon = TODO

  def follow(screenName: String) = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
          manageUserService.getUsersByUserIdList(userOption.get.follow).map { following =>
            Ok(views.html.followwithlogin(user, following))
          }
        }
      }
      case None => {
        manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
          manageUserService.getUsersByUserIdList(userOption.get.follow).map { following =>
            Ok(views.html.follow(following))
          }
        }
      }
    }
  }

  def makeFollow(screenName: String) = AsyncStack{ implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
          userService.makeFollow(user, userOption.get).map{ user =>
            Redirect(routes.RootController.profile(screenName))
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.profile(screenName)))
    }
  }

  def makeUnfollow(screenName: String) = AsyncStack{ implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
          userService.makeUnfollow(user, userOption.get).map{ user =>
            Redirect(routes.RootController.profile(screenName))
          }
        }
      }
      case None => Future.successful(Redirect(routes.RootController.profile(screenName)))
    }
  }
}
