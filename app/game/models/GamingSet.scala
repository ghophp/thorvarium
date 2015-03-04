package game.models

import models.{Person, Player}
import play.api.libs.json.{JsObject, JsValue}

class GamingSet(var movements : Map[String, GamingMove],
                val weapons: Map[String, Map[String, GamingWeapon]]) {

}

object GamingSet {

  /**
   * Sample user data
   *  {
   *     "persons": {
   *       "person1": {
   *         "x": 0,
   *         "y": 0,
   *         "weapon1": {
   *           "x": 0,
   *           "y": 0
   *         },
   *         "weapon2": {
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

        val w1 = (js._2 \ Person.WeaponSlot1).asOpt[JsObject]
        val w2 = (js._2 \ Person.WeaponSlot2).asOpt[JsObject]

        def xy(o:Option[JsObject]) : GamingWeapon = {
          if (o.isDefined) {
            val x : Double = (o.get \ "x").asOpt[Double].getOrElse(0)
            val y : Double = (o.get \ "y").asOpt[Double].getOrElse(0)
            new GamingWeapon(x, y)
          } else null
        }

        js._1 -> Map(Person.WeaponSlot1 -> xy(w1), Person.WeaponSlot2 -> xy(w2))

      }.toMap[String, Map[String, GamingWeapon]]

      return new GamingSet(moves, weapons)
    }

    null
  }
}

