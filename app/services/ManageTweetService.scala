package services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
  def getTweets: Future[List[Tweet]]

  def getTweets(num: Integer): Future[List[Tweet]]

  def getTweetsByUserId(userId: Long): Future[List[Tweet]]

  def getTweetsByUserIdList(userIdList: List[Long]): Future[List[Tweet]]
}

class ManageTweetWithElasticsearchService extends ManageTweetService with ManageElasticsearch {

  def config = ESConfig("twitter-clone", "tweet")

  def convertEngDateToNumDate(date: String): String = date match {
    case "Jun" => "1"
    case "Feb" => "2"
    case "Mar" => "3"
    case "Apr" => "4"
    case "May" => "5"
    case "Jun" => "6"
    case "Jul" => "7"
    case "Aug" => "8"
    case "Sep" => "9"
    case "Oct" => "10"
    case "Nov" => "11"
    case "Dec" => "12"
    case _ => date
  }

  def convertStringDateToDate(tweetDB: TweetDB): Tweet = {
    Tweet(
      id = tweetDB.id,
      user_id = tweetDB.user_id,
      text = tweetDB.text,
      created_at = LocalDateTime.parse(
        tweetDB.created_at.split(" ").drop(1).map(convertEngDateToNumDate).mkString(" "),
        DateTimeFormatter.ofPattern("MMM dd HH:mm:ss Z yyyy")
      ),
      retweet_count = tweetDB.retweet_count,
      favorite_count = tweetDB.favorite_count
    )
  }

  def mappingIdToLongInTweets(tweets: List[Tweet]) =
    tweets.map(tweet => tweet.copy(
        id = toLong(tweet.id),
        user_id = toLong(tweet.user_id)
      )
    )

  def getTweets: Future[List[Tweet]] = getTweets(1000)

  def getTweets(num: Integer): Future[List[Tweet]] = {
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
    AsyncESClient.apply(serverUrl).listAsync[TweetDB](config) { searcher =>
      searcher.setQuery(QueryBuilders.termsQuery("user_id", userIdList: _*))
        .setSize(1000)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
      .map(_.map(convertStringDateToDate))
      .map(mappingIdToLongInTweets)
}