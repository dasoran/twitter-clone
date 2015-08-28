package services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.google.inject.ImplementedBy
import jp.co.bizreach.elasticsearch4s._

import models.{TweetDB, Tweet}
import org.elasticsearch.index.query.QueryBuilders

import org.elasticsearch.search.sort.SortOrder

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by kyota.yasuda on 15/08/13.
 */

@ImplementedBy(classOf[ManageTweetWithElasticsearchService])
trait ManageTweetService {
  def getTweetById(id: Long): Future[Option[Tweet]]

  def getTweets: Future[List[Tweet]]

  def getTweets(num: Int): Future[List[Tweet]]

  def getTweetsByUserId(userId: Long): Future[List[Tweet]]

  def getTweetsByUserIdList(userIdList: List[Long]): Future[List[Tweet]]

  def getTweetsByUserIdList(userIdList: List[Long], num: Int): Future[List[Tweet]]

  def getReplyTweetsByUserIdList(userIdList: List[Long], screenName: String, num: Int): Future[List[Tweet]]

  def getTweetsByUserIdListToTheTweet(userIdList: List[Long], num: Int, lastId: Long): Future[List[Tweet]]

  def getReplyTweetsByUserIdListToTheTweet(userIdList: List[Long], screenName: String, num: Int, lastId: Long): Future[List[Tweet]]

  def insertTweet(tweet: Tweet): Future[Any]

  def updateTweet(tweet: Tweet): Future[Any]

  def deleteTweet(tweetId: Long): Future[Any]
}

class ManageTweetWithElasticsearchService extends ManageTweetService with ManageElasticsearch {

  def config = ESConfig("twitter-clone", "tweet")


  def convertStringDateToDate(tweetDB: TweetDB): Tweet = {
    Tweet(
      id = tweetDB.id,
      user_id = tweetDB.user_id,
      text = tweetDB.text,
      created_at = LocalDateTime.parse(
        tweetDB.created_at.split(" ").drop(1).map(convertEngDateToNumDate).mkString(" "),
        DateTimeFormatter.ofPattern("M dd HH:mm:ss Z yyyy")
      ),
      favorited_user_id = tweetDB.favorited_user_id
    )
  }

  def mappingIdToLongInTweets(tweets: List[Tweet]) =
    tweets.map(tweet => tweet.copy(
      id = toLong(tweet.id),
      user_id = toLong(tweet.user_id)
    )
    )

  def getTweetById(id: Long): Future[Option[Tweet]] = {
    AsyncESClient.apply(serverUrl).findAsync[TweetDB](config){ searcher =>
      searcher.setQuery(termQuery("_id", id))
    }.map(_.map(_._2))
      .map(_.map(convertStringDateToDate))
      .map(_.map(tweet => tweet.copy(
        id = toLong(tweet.id),
        user_id = toLong(tweet.user_id)
      )))
  }

  def getTweets: Future[List[Tweet]] = getTweets(1000)

  def getTweets(num: Int): Future[List[Tweet]] = {
    AsyncESClient.apply(serverUrl).listAsync[TweetDB](config) { searcher =>
      searcher
        .setSize(num)
        .addSort("created_at", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(_.map(convertStringDateToDate))
      .map(mappingIdToLongInTweets)
  }

  def getTweetsByUserId(userId: Long): Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).listAsync[TweetDB](config) { searcher =>
      searcher
        .setQuery(termQuery("user_id", userId))
        .setSize(2000)
        .addSort("created_at", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(_.map(convertStringDateToDate))
      .map(mappingIdToLongInTweets)

  def getTweetsByUserIdList(userIdList: List[Long]): Future[List[Tweet]] =
    getTweetsByUserIdList(userIdList, 1000)

  def getTweetsByUserIdList(userIdList: List[Long], num: Int): Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).listAsync[TweetDB](config) { searcher =>
      searcher.setQuery(QueryBuilders.termsQuery("user_id", userIdList: _*))
        .setSize(num)
        .addSort("created_at", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(_.map(convertStringDateToDate))
      .map(mappingIdToLongInTweets)


  def getReplyTweetsByUserIdList(userIdList: List[Long], screenName: String, num: Int): Future[List[Tweet]] = {
    AsyncESClient.apply(serverUrl).listAsync[TweetDB](config) { searcher =>
      searcher
        .setQuery(QueryBuilders.boolQuery()
          .must(QueryBuilders.matchQuery("text", "@" + screenName)))
        .setSize(num)
        .addSort("created_at", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(_.map(convertStringDateToDate))
      .map(mappingIdToLongInTweets)
    }

  def getTweetsByUserIdListToTheTweet(userIdList: List[Long], num: Int, lastId: Long): Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).findAsync[TweetDB](config){ searcher =>
      searcher.setQuery(termQuery("_id", lastId))
    }.map(_.map(_._2))
      .map(_.map(tweet => tweet.copy(
        id = toLong(tweet.id),
        user_id = toLong(tweet.user_id)
      )))
      .flatMap { lastTweet =>
        AsyncESClient.apply(serverUrl).listAsync[TweetDB](config) { searcher =>
      searcher
        .setQuery(QueryBuilders.boolQuery()
          .must(QueryBuilders.termsQuery("user_id", userIdList: _*))
          .must(QueryBuilders.rangeQuery("created_at").lt(lastTweet.get.created_at)))
        .setSize(num)
        .addSort("created_at", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
          .map(_.map(convertStringDateToDate))
          .map(mappingIdToLongInTweets)
      }


  def getReplyTweetsByUserIdListToTheTweet(userIdList: List[Long], screenName: String, num: Int, lastId: Long): Future[List[Tweet]] =
    AsyncESClient.apply(serverUrl).findAsync[TweetDB](config){ searcher =>
      searcher.setQuery(termQuery("_id", lastId))
    }.map(_.map(_._2))
      .map(_.map(tweet => tweet.copy(
        id = toLong(tweet.id),
        user_id = toLong(tweet.user_id)
      )))
      .flatMap { lastTweet =>
        AsyncESClient.apply(serverUrl).listAsync[TweetDB](config) { searcher =>
      searcher
        .setQuery(QueryBuilders.boolQuery()
          .must(QueryBuilders.rangeQuery("created_at").lt(lastTweet.get.created_at))
          .must(QueryBuilders.matchQuery("text", "@" + screenName)))
        .setSize(num)
        .addSort("created_at", SortOrder.DESC)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
          .map(_.map(convertStringDateToDate))
          .map(mappingIdToLongInTweets)
      }

  def insertTweet(tweet: Tweet): Future[Any] = {
    val toInsert = TweetDB(
        id = tweet.id,
        user_id = tweet.user_id,
        text = tweet.text,
        created_at = tweet.created_at
          .format(DateTimeFormatter.ofPattern("EEE MM dd HH:mm:ss +0000 yyyy").withLocale(Locale.ENGLISH))
          .split(" ").map(convertNumDateToEngDate).mkString(" "),
        favorited_user_id = tweet.favorited_user_id
    )
    AsyncESClient.apply(serverUrl).insertAsync(config, toInsert)
  }

  def updateTweet(tweet: Tweet): Future[Any] = {
    val toUpdate = TweetDB(
      id = tweet.id,
      user_id = tweet.user_id,
      text = tweet.text,
      created_at = tweet.created_at
        .format(DateTimeFormatter.ofPattern("EEE MM dd HH:mm:ss +0000 yyyy").withLocale(Locale.ENGLISH))
        .split(" ").map(convertNumDateToEngDate).mkString(" "),
      favorited_user_id = tweet.favorited_user_id
    )
    AsyncESClient.apply(serverUrl).updateAsync(config, toUpdate.id.toString, toUpdate)
  }


  def deleteTweet(tweetId: Long): Future[Any] = {
    AsyncESClient.apply(serverUrl).deleteAsync(config, tweetId.toString)
  }
}