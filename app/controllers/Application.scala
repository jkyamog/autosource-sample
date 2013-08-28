package controllers

import play.api._
import play.api.mvc._

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Done

import reactivemongo.api._
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection.JSONCollection

import play.autosource.reactivemongo._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

case class Person(name: String, age: Int)
object Person{
  implicit val fmt = Json.format[Person]
}

case class User(name: String)
object User {
  def find(name: String) = Some(User(name))
}

object Application extends ReactiveMongoAutoSourceController[Person] {
  def Authenticated(action: User => EssentialAction): EssentialAction = {
    // Let's define a helper function to retrieve a User
    def getUser(request: RequestHeader): Option[User] = {
      request.session.get("user").flatMap(u => User.find(u))
    }

    // Now let's define the new Action
    EssentialAction { request =>
      getUser(request).map(u => action(u)(request)).getOrElse {
        Done(Unauthorized)
      }
    }
  }

  val coll = db.collection[JSONCollection]("persons")

  override def insert: EssentialAction = Action(parse.json) { request =>
    Json.fromJson[Person](request.body)(reader).map { t =>
      Async {
        for {
          id <- res.insert(t)
          person <- res.get(id)
          if person.isDefined
        } yield (Ok(Json.toJson(person.get)(writerWithId)))
      }
    }.recoverTotal { e => BadRequest(JsError.toFlatJson(e)) }
  }

  override def update(id: BSONObjectID): EssentialAction = Action(parse.json) { request =>
    Json.fromJson[Person](request.body)(reader).map { t =>
      Async {
        res.update(id, t).map { _ => NoContent }
      }
    }.recoverTotal { e => BadRequest(JsError.toFlatJson(e)) }
  }

  def updateUsingAction(id: BSONObjectID): Action[JsValue] = Action(parse.json) { request =>
    Json.fromJson[Person](request.body)(reader).map { t =>
      Async {
        res.update(id, t).map { _ => NoContent }
      }
    }.recoverTotal { e => BadRequest(JsError.toFlatJson(e)) }
  }
  
  override def delete(id: BSONObjectID) = Authenticated { _ =>
    super.delete(id)
  }

  override def get(id: BSONObjectID) = Authenticated { _ =>
    super.get(id)
  }
  
	def addDefaultValuesToJson(json: JsValue) = {
		val addData = __.json.update(
				(__ \ 'foo).json.put(JsBoolean(false))
		)
		
		json.transform(addData).get
	}
  
  def brokenWithInvalidJson(id: String) = Action(parse.json.map(addDefaultValuesToJson)) { request =>
    Async {
    	update(new BSONObjectID(id))(request).run
    }
  }

  def working(id: String) = Action(parse.json.map(addDefaultValuesToJson)) { request =>
  	updateUsingAction(new BSONObjectID(id))(request)
  }
  
  def index = Action {
    Ok(views.html.index("ok"))
  }

  def login(name: String) = Action {
    Ok("logged in").withSession("user" -> name)
  }

  def logout = Action {
    Ok("logged out").withNewSession
  }
    
}