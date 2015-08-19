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

class ManageTweetWithElasticsearchService extends ManageTweetService with ManageElasticsearch {

  def config = ESConfig("twitter-clone", "tweet")

  def mappingIdToLongInTweets(tweets: List[Tweet]) =
    tweets.map(tweet => tweet.copy(
        id = toLong(tweet.id),
        user_id = toLong(tweet.user_id)
      )
    )

  def getTweets: Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).listAsync[Tweet](config) { searcher =>
      searcher
        .setSize(200)
        .addSort("id", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(mappingIdToLongInTweets)

  def getTweetsByUserId(userId: Long): Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).listAsync[Tweet](config) { searcher =>
      searcher
        .setQuery(termQuery("user_id", userId))
        .setSize(200)
        .addSort("id", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(mappingIdToLongInTweets)

  def getTweetsByUserIdList(userIdList: List[Long]): Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).listAsync[Tweet](config) { searcher =>
      searcher.setQuery(QueryBuilders.termsQuery("user_id", userIdList: _*))
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(mappingIdToLongInTweets)
}