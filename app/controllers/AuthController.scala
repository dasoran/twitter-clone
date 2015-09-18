package controllers

import java.security.SecureRandom
import javax.inject.Inject

import jp.t2v.lab.play2.auth.LoginLogout
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import models._
import models.forms.SignupForm
import services.UserService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random


class AuthController @Inject()(val messagesApi: MessagesApi,
                               val userService: UserService) extends Controller
with I18nSupport with LoginLogout with AuthConfigImpl {


  val emailPattern = Constraints.pattern("[\\w\\d._-]+@[\\w\\d_-]+\\.[\\w\\d._-]+".r)
  val signupForm = Form(
    mapping(
      "signupInputUserId1" -> nonEmptyText(maxLength = 20),
      "signupInputEmail1" -> nonEmptyText.verifying(emailPattern),
      "signupInputPassword1" -> nonEmptyText(maxLength = 50)
    )(SignupForm.apply)(SignupForm.unapply)
  )

  val loginForm = Form(
    mapping(
      "loginInputUserId1" -> nonEmptyText(maxLength = 20),
      "loginInputPassword1" -> nonEmptyText(maxLength = 50)
    )(userService.authenticate)(_.map(u => (u.screen_name, "")))
      .verifying("Invalid id or password", result => result.isDefined)
  )


  def signup = Action.async { implicit rs =>
    Future {
      Ok(views.html.signup(None))
    }
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future{
        BadRequest(views.html.welcome(Option("情報が間違っています。再度入力をお願いします。")))
      },
      user => gotoLoginSucceeded(user.get.id)
    )
  }


  def create = Action.async { implicit rs =>
    signupForm.bindFromRequest.fold(
      // エラーの場合
      error => {
        println(error.errorsAsJson)
        Future {
          BadRequest(views.html.signup(Option("入力されたデータが不正です。再度入力をお願いします。")))
        }
      },
      // OKの場合
      form => {
        val r = new Random(new SecureRandom())
        val userId = Math.abs(r.nextLong())
        val newUser = User(
          id = userId,
          password = Option(form.signupInputPassword1),
          screen_name = form.signupInputUserId1,
          name = form.signupInputUserId1,
          description = "",
          profile_image_url = "/img/default-icon.png",
          follow = List(),
          follower = List(),
          email = Option(form.signupInputEmail1)
        )

        userService.create(newUser).flatMap { f =>
          Future {
            Redirect(routes.RootController.index)
          }
        }
      }
    )
  }

}