package services


import javax.inject.Inject

import jp.co.bizreach.elasticsearch4s._
import org.mindrot.jbcrypt.BCrypt

import models._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
 * Created by kyota.yasuda on 15/08/17.
 */
class UserService @Inject()(val manageUserService: ManageUserService,
                            val manageTweetService: ManageTweetService) {

  def authenticate(screen_name: String, password: String): Option[User] = {
    val f = manageUserService.getUserByScreenName(screen_name)
      .map(userOption =>
        userOption.filter(user => user.password.isDefined)
          .filter(user => BCrypt.checkpw(password, user.password.get))
      )
    Await.result(f, Duration.Inf)
  }


  def create(user: User): Future[Any] = {
    val userForSave = user.copy(password = Option(BCrypt.hashpw(user.password.get, BCrypt.gensalt())))
    manageUserService.insertUser(userForSave)
  }

}
