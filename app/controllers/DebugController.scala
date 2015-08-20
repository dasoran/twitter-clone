package controllers

import javax.inject.Inject

import models.{Uservector, Tweet, User}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{ManageUservectorService, ManageTweetService, ManageUserService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DebugController @Inject()(
                                 val messagesApi: MessagesApi,
                                 val manageUserService: ManageUserService,
                                 val manageTweetService: ManageTweetService,
                                 val manageUservectorService: ManageUservectorService)
  extends Controller with I18nSupport {

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

  def getGraph = Action.async { implicit rs =>
    manageTweetService.getTweets
      .flatMap { tweets =>
        val pattern = ".*@(\\w+)\\s.*".r
        val replyScreenNames = tweets.map { tweet =>
          tweet.text match {
            case pattern(screen_name) => Option(tweet, screen_name)
            case _ => None
          }
        }.filter(_.isDefined).map(_.get)

        val futures: Seq[Future[Option[(Tweet, String, User)]]] = replyScreenNames.map { case (tweet, screen_name) =>
          manageUserService.getUserByScreenName(screen_name).map {
            case Some(x) => Option((tweet, screen_name, x))
            case None => None
          }
        }
        Future.fold(futures)(List(): List[Option[(Tweet, String, User)]]) { (users, user) => user :: users }
          .map { users =>
            val uservecs: List[Uservector] = users.filter(user => user.isDefined).map(_.get)
            .map { case (tweet, screen_name, toUser) =>
              (tweet.user_id, toUser.id)
            }.foldLeft(Map(): Map[Long, Map[Long, Int]]) {
              (relationMap: Map[Long, Map[Long, Int]], relation: (Long, Long)) =>
                val next: Map[Long, Map[Long, Int]] = relationMap.get(relation._1) match {
                  case Some(prevRelations: Map[Long, Int]) => {
                    prevRelations.get(relation._2) match {
                      case Some(x) => Map(relation._1 -> (prevRelations ++ Map(relation._2 -> (x + 1))))
                      case None => Map(relation._1 -> (prevRelations ++ Map(relation._2 -> 1)))
                    }
                  }
                  case None => Map(relation._1 -> Map(relation._2 -> 1))
                }
                relationMap ++ next
            }.foldLeft(List(): List[Uservector]){ (uservecs, relations) =>
              val uservec = Uservector(relations._1, relations._2.maxBy(_._2)._1)
              manageUservectorService.insertUservector(uservec)
              uservec :: uservecs
            }

            Ok(views.html.debug.getgraph(uservecs))
          }
      }
  }
}

