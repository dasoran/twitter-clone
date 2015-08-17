package services

import com.google.inject.ImplementedBy
import jp.co.bizreach.elasticsearch4s._

import models.Tweet
import org.elasticsearch.index.query.QueryBuilders

import org.elasticsearch.search.sort.SortOrder

/**
 * Created by kyota.yasuda on 15/08/13.
 */

@ImplementedBy(classOf[ManageTweetWithElasticsearchService])
trait ManageTweetService {
  def getTweets: List[Tweet]
  def getTweetsByUserId(userId: Long): List[Tweet]
  def getTweetsByUserIdList(userIdList: List[Long]): List[Tweet]
}

class ManageTweetWithElasticsearchService extends ManageTweetService {
  def getTweets: List[Tweet] = {
    ESClient.init()
    val rawTweets: ESSearchResult[Tweet] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "tweet")
      client.list[Tweet](config) { searcher =>
        searcher.setSize(200)
        searcher.addSort("id", SortOrder.DESC)
      }
    }
    ESClient.shutdown()
    rawTweets.list.filter(_.doc.id != 0).map(_.doc)
  }

  def getTweetsByUserId(userId: Long): List[Tweet] = {
    ESClient.init()
    val rawTweets: ESSearchResult[Tweet] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "tweet")
      client.list[Tweet](config) { searcher =>
        searcher.setQuery(termQuery("user_id", userId))
        searcher.setSize(200)
        searcher.addSort("id", SortOrder.DESC)
      }
    }
    ESClient.shutdown()
    rawTweets.list.filter(_.doc.id != 0).map(_.doc)
  }

  def getTweetsByUserIdList(userIdList: List[Long]): List[Tweet] = {
    ESClient.init()
    val rawTweets: ESSearchResult[Tweet] = ESClient.using("http://localhost:9200/") { client =>
      val config = ESConfig("twitter-clone", "tweet")
      client.list[Tweet](config) { searcher =>
        searcher.setQuery(QueryBuilders.termsQuery("user_id", userIdList: _*))
      }
    }
    ESClient.shutdown()
    rawTweets.list.filter(_.doc.id != 0).map(_.doc)
  }
}