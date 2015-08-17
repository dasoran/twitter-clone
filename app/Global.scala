import jp.co.bizreach.elasticsearch4s.ESClient
import play.api.{Application, GlobalSettings, Play}

//import utils.ElasticsearchUtil

object Global extends GlobalSettings {

  lazy val logger = play.api.Logger(this.getClass)

  override def onStart(app: Application): Unit =
    try {
      super.onStart(app)

      //ElasticsearchUtil.init(Play.current)
      ESClient.init()

    } catch {
      case ex: Throwable =>
        if (!Play.isDev(Play.current))
          logger.error("Starting up process failed !!!", ex)
        throw ex
    }

  override def onStop(app: Application) {
    ESClient.shutdown()
    logger.info("Application shutdown...")
  }
}
