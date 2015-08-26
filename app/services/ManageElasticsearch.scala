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


  def convertEngDateToNumDate(date: String): String = date match {
    case "Jan" => "1"
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

  def convertNumDateToEngDate(date: String): String = date match {
    case "01" => "Jan"
    case "02" => "Feb"
    case "03" => "Mar"
    case "04" => "Apr"
    case "05" => "May"
    case "06" => "Jun"
    case "07" => "Jul"
    case "08" => "Aug"
    case "09" => "Sep"
    case "10" => "Oct"
    case "11" => "Nov"
    case "12" => "Dec"
    case _ => date
  }


}
