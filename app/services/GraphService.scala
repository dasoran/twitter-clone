package services

import java.security.SecureRandom

import com.google.inject.Inject
import models.{Group, Uservector, User, Tweet}

import scala.concurrent.Future
import scala.util.Random

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by dasoran on 2015/08/22.
 */
class GraphService @Inject()(val manageUserService: ManageUserService,
                             val manageTweetService: ManageTweetService,
                             val manageUservectorService: ManageUservectorService,
                             val manageGroupService: ManageGroupService) {
  def createGraph: Future[(List[Uservector], List[Group])] = {
    manageGroupService.deleteAllGroups.flatMap { f =>
      manageTweetService.getTweets(2000)  // TODO 直近30分に限定する
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
              }.foldLeft(List(): List[Uservector]) { (uservecs, relations) =>
                val uservec = Uservector(relations._1, relations._2.maxBy(_._2)._1)
                manageUservectorService.insertUservector(uservec)
                uservec :: uservecs
              }

              val rawGroups = uservecs.foldLeft(Set(): Set[Set[Long]]) { (groups, uservec) =>
                val subGroups = groups.filter { group =>
                  group.contains(uservec.id) || group.contains(uservec.to)
                }
                subGroups.size match {
                  case 0 => groups + Set(uservec.id, uservec.to)
                  case 1 => (groups -- subGroups) ++ subGroups.map(_ ++ Set(uservec.id, uservec.to))
                  case 2 => (groups -- subGroups) + subGroups.foldLeft(Set(): Set[Long]) { (sub, group) =>
                    sub ++ group
                  }
                }
              }

              val groups: List[Group] = rawGroups.map { rawGroup =>
                val r = new Random(new SecureRandom())
                val groupId = Math.abs(r.nextLong())
                val group = Group(groupId, rawGroup)
                manageGroupService.insertGroup(group)
                group
              }.toList.sortBy(group => group.users.size)

              (uservecs, groups)
            }
        }
    }
  }
}
