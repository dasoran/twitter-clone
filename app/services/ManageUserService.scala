package services

import com.google.inject.ImplementedBy
import jp.co.bizreach.elasticsearch4s._

import models.User
import org.elasticsearch.index.query.QueryBuilders

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by kyota.yasuda on 15/08/13.
 */
@ImplementedBy(classOf[ManageUserWithElasticsearchService])
trait ManageUserService {
  def getUserById(id: Long): Future[Option[User]]

  def getUserByScreenName(screenName: String): Future[Option[User]]

  def getUsers: Future[List[User]]

  def getUsersByUserIdList(userIdList: List[Long]): Future[List[User]]

  def insertUser(user: User): Future[Any]

  def updateUser(user: User): Future[Any]
}

class ManageUserWithElasticsearchService extends ManageUserService with ManageElasticsearch {

  val config = ESConfig("twitter-clone", "user")

  private def mapping(user: User) = user.copy(
    id = toLong(user.id),
    follow = user.follow.map(toLong),
    follower = user.follower.map(toLong)
  )

  def mappingIdToLongInUser(users: Option[User]) = users.map(mapping)

  def mappingIdToLongInUsers(users: List[User]) = users.map(mapping)


  def getUserById(id: Long): Future[Option[User]] =
    AsyncESClient.apply(serverUrl).findAsync[User](config){ searcher =>
      searcher.setQuery(termQuery("_id", id))
    }.map(_.map(_._2))
      .map(mappingIdToLongInUser)


  def getUserByScreenName(screenName: String): Future[Option[User]] =
    AsyncESClient.apply(serverUrl).findAsync[User](config) { searcher =>
      searcher.setQuery(termQuery("screen_name", screenName))
    }.map(_.map(_._2))
      .map(mappingIdToLongInUser)


  def getUsers: Future[List[User]] =
    AsyncESClient.apply(serverUrl).listAsync[User](config){ searcher =>
      searcher.setSize(200)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(mappingIdToLongInUsers)


  def getUsersByUserIdList(userIdList: List[Long]): Future[List[User]] =
    AsyncESClient.apply(serverUrl).listAsync[User](config){ searcher =>
      searcher
        .setQuery(QueryBuilders.termsQuery("id", userIdList: _*))
        .setSize(200)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(mappingIdToLongInUsers)

  // insertだから待つ必要なし
  // もしかしたらあとでuniqueチェック的なものでbool返す必要が有るかもしれない。それをするなら待たないといけないかも....
  def insertUser(user: User): Future[Any] = {
    AsyncESClient.apply(serverUrl).insertAsync(config, user)

    /* 検証用 */
    .flatMap{f =>
      AsyncESClient.apply(serverUrl).listAsync[User](config){ searcher =>
        searcher.setSize(200)
      }.flatMap { users =>
        val followList = users.list.filter(_.doc.id != 0).map(_.doc).map { targetUser =>
          val toUpdate = targetUser.copy(follower = user.id :: targetUser.follower)
          AsyncESClient.apply(serverUrl).updateAsync(config, toUpdate.id.toString, toUpdate)
          targetUser.id
        }
        val toUpdate = user.copy(follow = followList)
        AsyncESClient.apply(serverUrl).updateAsync(config, toUpdate.id.toString, toUpdate)
        /* 検証用 */
      }
    }
  }

  def updateUser(user: User): Future[Any] = {
    AsyncESClient.apply(serverUrl).updateAsync(config, user.id.toString, user)
  }
}
