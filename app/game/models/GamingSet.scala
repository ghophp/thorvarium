package game.models

import models.{Person, Player}
import play.api.libs.json.{Json, JsObject, JsValue}

class GamingSet(var movements : Map[String, GamingMove],
                val weapons: Map[String, (String, GamingWeapon)]) {
  def toJson = {
    Json.obj(
      "movements" -> (
        if (movements != null)
          Json.toJson(movements.map( p => p._1 -> p._2.toJson ))
        else
          Json.obj()
        ),
      "weapons" -> (
        if (weapons != null)
          Json.toJson(weapons.map( p =>
            p._1 -> (if (p._2 != null) Json.obj(p._2._1 -> p._2._2.toJson) else Json.obj())
          ))
        else
          Json.obj()
        )
    )
  }
}

object GamingSet {

  /**
   * Sample user data
   *  {
   *     "persons": {
   *       "person1": {
   *         "x": 0,
   *         "y": 0,
   *         "weapon": {
   *           "id": 1,
   *           "x": 0,
   *           "y": 0
   *         }
   *       }
   *     }
   *   }
   * @param command user json
   * @return mapped player options
   */
  def toTurnSet(command : JsValue) : GamingSet = {

    val personsJs = (command \ "persons").asOpt[JsObject] match {
      case Some(j) => j.fieldSet.filter( p => Player.PersonSlots.contains(p._1) )
      case None => Set.empty
    }

    if (personsJs.size > 0) {

      val moves = personsJs.map { js =>
        val x : Double = (js._2 \ "x").asOpt[Double].getOrElse(0)
        val y : Double = (js._2 \ "y").asOpt[Double].getOrElse(0)
        js._1 -> new GamingMove(x, y)
      }.toMap

      val weapons = personsJs.map { js =>

        val w = (js._2 \ "weapon").asOpt[JsObject]
        var wp : (String, GamingWeapon) = null

        w match {
          case Some(o) =>
            val slot : String = (w.get \ "slot").asOpt[String].getOrElse("")
            if (!slot.isEmpty && Person.WeaponSlots.contains(slot)) {
              val x : Double = (w.get \ "x").asOpt[Double].getOrElse(0)
              val y : Double = (w.get \ "y").asOpt[Double].getOrElse(0)
              wp = slot -> new GamingWeapon(x, y)
            }
          case None => wp = null
        }

        js._1 -> wp

      }.toMap[String, (String, GamingWeapon)]
      return new GamingSet(moves, weapons)
    }

    null
  }
}

