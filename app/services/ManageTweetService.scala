package services

import com.google.inject.ImplementedBy
import jp.co.bizreach.elasticsearch4s._

import models.Tweet
import org.elasticsearch.index.query.QueryBuilders

import org.elasticsearch.search.sort.SortOrder

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by kyota.yasuda on 15/08/13.
 */

@ImplementedBy(classOf[ManageTweetWithElasticsearchService])
trait ManageTweetService {
  def getTweets: Future[List[Tweet]]

  def getTweetsByUserId(userId: Long): Future[List[Tweet]]

  def getTweetsByUserIdList(userIdList: List[Long]): Future[List[Tweet]]
}

class ManageTweetWithElasticsearchService extends ManageTweetService {

  def toLong: Any => Long = {
    case x: Integer => x.toLong
    case x: Long => x
  }

  def serverUrl = "http://localhost:9200"

  def config = ESConfig("twitter-clone", "tweet")

  def getTweets: Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).listAsync[Tweet](config) { searcher =>
      searcher.setSize(200)
      searcher.addSort("id", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(_.map(tweet => tweet.copy(id = toLong(tweet.id), user_id = toLong(tweet.user_id))))

  def getTweetsByUserId(userId: Long): Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).listAsync[Tweet](config) { searcher =>
      searcher.setQuery(termQuery("user_id", userId))
      searcher.setSize(200)
      searcher.addSort("id", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(_.map(tweet => tweet.copy(id = toLong(tweet.id), user_id = toLong(tweet.user_id))))

  def getTweetsByUserIdList(userIdList: List[Long]): Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).listAsync[Tweet](config) { searcher =>
      searcher.setQuery(QueryBuilders.termsQuery("user_id", userIdList: _*))
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(_.map(tweet => tweet.copy(id = toLong(tweet.id), user_id = toLong(tweet.user_id))))
}