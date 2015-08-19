import jp.co.bizreach.elasticsearch4s.{AsyncESClient, ESClient}
import play.api.{Application, GlobalSettings, Play}


object Global extends GlobalSettings {

  lazy val logger = play.api.Logger(this.getClass)

  override def onStart(app: Application): Unit =
    try {
      super.onStart(app)

      AsyncESClient.init()

    } catch {
      case ex: Throwable =>
        if (!Play.isDev(Play.current))
          logger.error("Starting up process failed !!!", ex)
        throw ex
    }

  override def onStop(app: Application) {
    AsyncESClient.shutdown()

    logger.info("Application shutdown...")
  }
}
