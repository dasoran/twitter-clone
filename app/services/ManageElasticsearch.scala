package services

/**
 * Created by kyota.yasuda on 15/08/19.
 */
trait ManageElasticsearch {

  def toLong: Any => Long = {
    case x: Integer => x.toLong
    case x: Long => x
  }

  val serverUrl = "http://localhost:9200"

}
