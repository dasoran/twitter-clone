package services

import com.google.inject.ImplementedBy
import jp.co.bizreach.elasticsearch4s._

import models.User
import org.elasticsearch.index.query.QueryBuilders

/**
 * Created by kyota.yasuda on 15/08/13.
 */
@ImplementedBy(classOf[ManageUserWithElasticsearchService])
trait ManageUserService {
  def getUserById(id: Long): Option[User]
  def getUserByScreenName(screenName: String): Option[User]
  def getUsers: List[User]
  def getUsersByUserIdList(userIdList: List[Long]): List[User]
}

class ManageUserWithElasticsearchService extends ManageUserService {
  def getUserById(id: Long): Option[User] = {
    ESClient.init()
    val rawUserOption: Option[(String, User)] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      client.find[User](config){ searcher =>
        searcher.setQuery(termQuery("_id", id))
      }
    }
    ESClient.shutdown()
    rawUserOption.map(_._2)
  }

  def getUserByScreenName(screenName: String): Option[User] = {
    ESClient.init()
    val rawUserOption: Option[(String, User)]  = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      client.find[User](config) { searcher =>
        searcher.setQuery(termQuery("screen_name", screenName))
      }
    }
    ESClient.shutdown()
    rawUserOption.map(_._2)
  }

  def getUsers: List[User] = {
    ESClient.init()
    val rawUsers: ESSearchResult[User] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      client.list[User](config){ searcher =>
        searcher.setSize(200)
      }
    }
    ESClient.shutdown()
    rawUsers.list.filter(_.doc.id != 0).map(_.doc)
  }

  def getUsersByUserIdList(userIdList: List[Long]): List[User] = {
    ESClient.init()
    val rawUsers: ESSearchResult[User] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      client.list[User](config){ searcher =>
        searcher.setQuery(QueryBuilders.termsQuery("id", userIdList: _*))
        searcher.setSize(200)
      }
    }
    ESClient.shutdown()
    rawUsers.list.filter(_.doc.id != 0).map(_.doc)
  }
}
