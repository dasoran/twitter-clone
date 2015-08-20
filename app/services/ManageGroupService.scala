package services

import com.google.inject.ImplementedBy

import jp.co.bizreach.elasticsearch4s._

import models.Group
import org.elasticsearch.index.query.QueryBuilders

import org.elasticsearch.search.sort.SortOrder

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by kyota.yasuda on 15/08/20.
 */
@ImplementedBy(classOf[ManageGroupWithElasticsearchService])
trait ManageGroupService {

  def insertGroup(group: Group): Future[Any]

  def deleteGroup(groupId: Long): Future[Any]

  def deleteAllGroups: Future[Any]

}


class ManageGroupWithElasticsearchService extends ManageGroupService with ManageElasticsearch {

  def config = ESConfig("nekomimi", "group")

  def insertGroup(group: Group): Future[Any] = {
    AsyncESClient.apply(serverUrl).insertAsync(config, group)
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

  def getGroups(num: Integer): Future[List[Group]] = {
    AsyncESClient.apply(serverUrl).listAsync[Group](config) { searcher =>
      searcher
        .setSize(num)
    }.map(_.list.filter(_.doc.id != 0).map(_.doc))
  }
}

