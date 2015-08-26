package services

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.google.inject.ImplementedBy

import jp.co.bizreach.elasticsearch4s._

import models.{GroupDB, Group}
import org.elasticsearch.index.query.QueryBuilders

import org.elasticsearch.search.sort.SortOrder

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by kyota.yasuda on 15/08/20.
 */
@ImplementedBy(classOf[ManageGroupWithElasticsearchService])
trait ManageGroupService {

  def getGroupById(groupId: Long): Future[Option[Group]]

  def getGroups(num: Int): Future[List[Group]]

  def insertGroup(group: Group): Future[Any]

  def deleteGroup(groupId: Long): Future[Any]

  def deleteAllGroups: Future[Any]

  def deleteOldGroups: Future[Any]

}


class ManageGroupWithElasticsearchService extends ManageGroupService with ManageElasticsearch {

  def config = ESConfig("nekomimi", "group")

  def convertStringDateToDate(groupDB: GroupDB): Group = {
    Group(
      id = groupDB.id,
      users = groupDB.users,
      created_at = LocalDateTime.parse(
        groupDB.created_at.split(" ").drop(1).map(convertEngDateToNumDate).mkString(" "),
        DateTimeFormatter.ofPattern("M dd HH:mm:ss Z yyyy")
      )
    )
  }


  def getGroupById(groupId: Long): Future[Option[Group]] = {
    AsyncESClient.apply(serverUrl).findAsync[GroupDB](config){ searcher =>
      searcher.setQuery(termQuery("_id", groupId))
    }.map(_.map{ rawGroup =>
      val group = rawGroup._2
      group.copy(users = group.users.map(toLong))
    }).map(_.map(convertStringDateToDate))
  }

  def insertGroup(group: Group): Future[Any] = {
    val toInsert = GroupDB(
      id = group.id,
      users = group.users,
      created_at = group.created_at
        .format(DateTimeFormatter.ofPattern("EEE MM dd HH:mm:ss +0000 yyyy").withLocale(Locale.ENGLISH))
        .split(" ").map(convertNumDateToEngDate).mkString(" ")
    )
    AsyncESClient.apply(serverUrl).insertAsync(config, toInsert)
  }

  def deleteGroup(groupId: Long): Future[Any] = {
    AsyncESClient.apply(serverUrl).deleteAsync(config, groupId.toString)
  }

  def deleteAllGroups: Future[Any] = {
    getGroups(200).flatMap{ groups =>
      val futures: Seq[Future[Any]] = groups.map { group =>
        deleteGroup(group.id)
      }
      Future.fold(futures)(Unit) { (unit, any) =>  unit}
    }
  }

  def getGroups(num: Int): Future[List[Group]] = {
    AsyncESClient.apply(serverUrl).listAsync[GroupDB](config) { searcher =>
      searcher
        .setSize(num)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc).map(convertStringDateToDate))
  }

  def deleteOldGroups: Future[Any] = {
    AsyncESClient.apply(serverUrl).listAsync[GroupDB](config) { searcher =>
      searcher
        .setSize(200)
        .setQuery(QueryBuilders.rangeQuery("created_at").lt(
          LocalDateTime.now().plusHours(-9).plusMinutes(-30)
            .format(DateTimeFormatter.ofPattern("EEE MM dd HH:mm:ss +0000 yyyy")
            .withLocale(Locale.ENGLISH))
            .split(" ").map(convertNumDateToEngDate).mkString(" ")
        ))
    }.map(_.list.filter(_.doc.id != 0).map(_.doc).map(convertStringDateToDate)).flatMap{ groups =>
      val futures: Seq[Future[Any]] = groups.map { group =>
        deleteGroup(group.id)
      }
      Future.fold(futures)(Unit) { (unit, any) =>  unit}
    }
  }
}

