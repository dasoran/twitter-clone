package controllers

import java.security.SecureRandom
import javax.inject.Inject

import jp.co.bizreach.elasticsearch4s._
import jp.t2v.lab.play2.auth.LoginLogout
import models._
import models.forms.{LoginForm, SignupForm}
import play.api.data._
import play.api.data.Forms._
import validation.Constraints._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random


class AuthController @Inject()(val messagesApi: MessagesApi) extends Controller
with I18nSupport with LoginLogout with AuthConfigImpl{


  val signupForm = Form(
    mapping(
      "signupInputUserId1" -> nonEmptyText(maxLength = 20),
      "signupInputEmail1" -> nonEmptyText(maxLength = 50),
      "signupInputPassword1" -> nonEmptyText(maxLength = 50)
    )(SignupForm.apply)(SignupForm.unapply)
  )

  val loginForm = Form(
    mapping(
      "loginInputUserId1" -> nonEmptyText(maxLength = 20),
      "loginInputPassword1" -> nonEmptyText(maxLength = 50)
    )(User.authenticate)(_.map(u => (u.screen_name, "")))
      .verifying("Invalid id or password", result => result.isDefined)
  )


  /**
   * 一覧表示
   */
  def signup = Action.async { implicit rs =>
    Future {
      Ok(views.html.signup(signupForm))
    }
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def authenticate = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      user => gotoLoginSucceeded(user.get.id)
    )
  }


  def create = Action.async { implicit rs =>
    signupForm.bindFromRequest.fold(
      // エラーの場合
      error => {
        //BadRequest(views.html.signup(error, signupForm))
        println(error.errorsAsJson)
        Future {
          BadRequest(views.html.signup(signupForm))
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

        User.create(newUser)
        Future {
          Redirect(routes.RootController.index)
        }
      }
    )
  }

}