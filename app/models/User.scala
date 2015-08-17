package models

import jp.co.bizreach.elasticsearch4s._
import org.mindrot.jbcrypt.BCrypt

import javax.inject.Inject

/**
 * Created by kyota.yasuda on 15/08/13.
 */
case class User(id: Long,
                password: Option[String] = None,
                screen_name: String,
                name: String,
                description: String,
                profile_image_url: String,
                follow: List[Long],
                follower: List[Long],
                email: Option[String] = None)

object User {

  def authenticate(screen_name: String, password: String): Option[User] = {
    ESClient.init()
    val userOption: Option[(String, User)] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      client.find[User](config){ searcher =>
        searcher.setQuery(termQuery("screen_name", screen_name))
      }
    }
    ESClient.shutdown()
    userOption.map(_._2)
      .filter { user => user.password.isDefined}
      .filter { user => BCrypt.checkpw(password, user.password.get) }
  }


  def create(user: User): Unit = {
    val userForSave = user.copy(password = Option(BCrypt.hashpw(user.password.get, BCrypt.gensalt())))

    ESClient.init()
    ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      client.insert(config, userForSave)
    }
    /* 検証用 */
    val rawUsers: ESSearchResult[User] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      client.list[User](config){ searcher =>
        searcher.setSize(200)
      }
    }

    val followList: List[Long] = rawUsers.list.map(_.doc).map { targetUser =>
      ESClient.using("http://localhost:9200/") { client =>
        val config = ESConfig("twitter-clone", "user")
        val toUpdate = targetUser.copy(follower = user.id :: targetUser.follower)
        client.update(config, toUpdate.id.toString, toUpdate)
      }
      targetUser.id
    }

    ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      val toUpdate = userForSave.copy(follow = followList)
      client.update(config, toUpdate.id.toString, toUpdate)
    }
    /* 検証用 */
    ESClient.shutdown()
  }

  def findById(id: Long): Option[User] = {
    ESClient.init()
    val userOption: Option[(String, User)] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "user")
      client.find[User](config){ searcher =>
        searcher.setQuery(termQuery("id", id))
      }
    }
    ESClient.shutdown()
    Option(userOption.get._2)
  }
}
