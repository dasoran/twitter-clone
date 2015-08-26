package controllers

import jp.t2v.lab.play2.auth.OptionalAuthElement
import models._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.i18n.{MessagesApi, I18nSupport}


import javax.inject.Inject
import services.{GraphService, UserService, ManageTweetService, ManageUserService}

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class RootController @Inject()(
                                val messagesApi: MessagesApi,
                                val manageUserService: ManageUserService,
                                val manageTweetService: ManageTweetService,
                                val userService: UserService,
                                val graphService: GraphService) extends Controller
with I18nSupport with OptionalAuthElement with AuthConfigImpl {


  def index = AsyncStack { implicit rs =>
    loggedIn match {
      case Some(user) => {
        manageTweetService.getTweetsByUserIdList(user.follow).flatMap { tweets =>
          manageUserService.getUsersByUserIdList(tweets.map(_.user_id)).flatMap { users =>
            val tweetsWithUser = tweets.map { tweet =>
              (tweet, users.find(user => user.id == tweet.user_id))
            }.filter { case (tweet, user) => user.isDefined }
              .map { case (tweet, user) => (tweet, user.get) }

            graphService.createGraph.flatMap { case (uservecs, groups) =>
              val futures: List[Future[(Long, List[String], List[User], List[(Tweet, User)])]] =
                groups.filter(_.users.size > 1).map { group =>
                  graphService.createIndex(group).flatMap { indexes =>
                    manageTweetService.getTweetsByUserIdList(group.users.toList, 5).flatMap { tweets =>
                      manageUserService.getUsersByUserIdList(group.users.toList).map { users =>
                        (group.id, indexes.take(5), users, tweets.map { tweet =>
                          (tweet, users.find(user => user.id == tweet.user_id))
                        }.filter { case (tweet, user) => user.isDefined }
                          .map { case (tweet, user) => (tweet, user.get) })
                      }
                    }
                  }
                }

              Future.fold(futures)(List(): List[(Long, List[String], List[User], List[(Tweet, User)])]) { (tweets, tweet) => tweet :: tweets }
                .map { tweetsOnGroup =>
                  Ok(views.html.index(tweetsWithUser, tweetsOnGroup))
                }
            }
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
    manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
      userOption match {
        case Some(targetUser) => {
          manageTweetService.getTweetsByUserId(targetUser.id).map { tweets =>
            loggedIn match {
              case Some(user) => Ok(views.html.profilewithlogin(user, targetUser, tweets.map((_, targetUser))))
              case None => Ok(views.html.profile(targetUser, tweets.map((_, targetUser))))
            }
          }
        }
        case None => Future(
          Ok(views.html.notfound("ユーザーが存在しません", "そのユーザーは存在しません。IDを確認してください。"))
        )
      }
    }
  }

  def favicon = TODO

  def follow(screenName: String) = AsyncStack {
    implicit rs =>
      manageUserService.getUserByScreenName(screenName).flatMap {
        userOption =>
          manageUserService.getUsersByUserIdList(userOption.get.follow).map {
            follow =>
              loggedIn match {
                case Some(user) => Ok(views.html.followwithlogin(user, follow))
                case None => Ok(views.html.follow(follow))
              }
          }
      }
  }

  def follower(screenName: String) = AsyncStack {
    implicit rs =>
      manageUserService.getUserByScreenName(screenName).flatMap {
        userOption =>
          manageUserService.getUsersByUserIdList(userOption.get.follower).map {
            follower =>
              loggedIn match {
                case Some(user) => Ok(views.html.followwithlogin(user, follower))
                case None => Ok(views.html.follow(follower))
              }
          }
      }
  }

  def makeFollow(screenName: String) = AsyncStack {
    implicit rs =>
      loggedIn match {
        case Some(user) => {
          manageUserService.getUserByScreenName(screenName).flatMap {
            userOption =>
              userService.makeFollow(user, userOption.get).map {
                user =>
                  Thread.sleep(1000)
                  Redirect(routes.RootController.profile(screenName))
              }
          }
        }
        case None => Future.successful(Redirect(routes.RootController.profile(screenName)))
      }
  }

  def makeUnfollow(screenName: String) = AsyncStack {
    implicit rs =>
      loggedIn match {
        case Some(user) => {
          manageUserService.getUserByScreenName(screenName).flatMap {
            userOption =>
              userService.makeUnfollow(user, userOption.get).map {
                user =>
                  Thread.sleep(1000)
                  Redirect(routes.RootController.profile(screenName))
              }
          }
        }
        case None => Future.successful(Redirect(routes.RootController.profile(screenName)))
      }
  }

  def edit(screenName: String) = AsyncStack { implicit rs =>
    manageUserService.getUserByScreenName(screenName).flatMap { userOption =>
      userOption match {
        case Some(targetUser) => {
          manageTweetService.getTweetsByUserId(targetUser.id).map { tweets =>
            loggedIn match {
              case Some(user) => {
                user match {
                  case x if x == targetUser => Ok(views.html.profileedit(user, targetUser, tweets.map((_, targetUser))))
                  case _ => Ok(views.html.notfound("権限がありません", "ユーザー情報は本人しか編集できません。"))
                }
              }
              case None => Ok(views.html.notfound("権限がありません", "ユーザー情報はログインしなければ編集できません。"))
            }
          }
        }
        case None => Future(
          Ok(views.html.notfound("ユーザーが存在しません", "そのユーザーは存在しません。IDを確認してください。"))
        )
      }
    }
  }

  case class ProfileForm(name: String, screen_name: String, profile_image_url: String, profile_text: String)

  val profileForm = Form(
    mapping(
      "name" -> nonEmptyText(maxLength = 50),
      "screen_name" -> nonEmptyText(maxLength = 20),
      "profile_image_url" -> nonEmptyText(maxLength = 200),
      "profile_text" -> nonEmptyText(maxLength = 200)
    )(ProfileForm.apply)(ProfileForm.unapply)
  )

  def update(screenName: String) = AsyncStack{implicit rs =>
    profileForm.bindFromRequest.fold(
      formWithErrors => Future(Ok(views.html.notfound("更新に失敗しました", "入力された値が不正です。"))),
      user => {
        Future(Redirect(routes.RootController.profile(screenName)))
      }
    )
  }
}
