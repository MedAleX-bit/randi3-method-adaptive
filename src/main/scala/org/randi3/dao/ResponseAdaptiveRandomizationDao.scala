package org.randi3.dao


import scala.slick.session.Database.threadLocalSession

import org.randi3.randomization.ResponseAdaptiveRandomization
import scalaz._
import org.randi3.schema.{DatabaseSchema, AdaptiveRandomizationSchema}
import scala.slick.driver.ExtendedProfile
import scala.slick.session.Database
import scala.slick.lifted.Parameters

class ResponseAdaptiveRandomizationDao(database: Database, driver: ExtendedProfile) extends AbstractRandomizationMethodDao(database, driver) {

  import driver.Implicit._

  val schemaCore = new DatabaseSchema(driver)

  import schemaCore._

  val schemaAdaptive = new AdaptiveRandomizationSchema(driver)

  import schemaAdaptive._

  private val queryAdaptiveRandomizationFromId = for {
    id <- Parameters[Int]
    adaptiveRandomization <- AdaptiveRandomizations if adaptiveRandomization.randomizationMethodId is id
  } yield adaptiveRandomization.*


  def create(randomizationMethod: ResponseAdaptiveRandomization, trialId: Int): Validation[String, Int] = {
    database withSession {
      val identifier =
        threadLocalSession withTransaction {
          val seed = randomizationMethod.random.nextLong()
          randomizationMethod.random.setSeed(seed)
          RandomizationMethods.noId insert(trialId, generateBlob(randomizationMethod.random).get, randomizationMethod.getClass().getName(), seed)
          val id = getId(trialId).toEither match {
            case Left(x) => return Failure(x)
            case Right(id1) => id1
          }
          if (randomizationMethod.isInstanceOf[ResponseAdaptiveRandomization]) {
            AdaptiveRandomizations.noId insert(0,
              Some(id),
              randomizationMethod.initialCountBalls,
              randomizationMethod.countBallsResponseSuccess,
              randomizationMethod.countBallsResponseFailure)
          }

          id
        }
      Success(identifier)
    }
  }

  def get(id: Int): Validation[String, Option[ResponseAdaptiveRandomization]] = {
    database withSession {
      generateRandomizationMethodFromDatabaseEntry(queryRandomizationMethodFromId(id).list)

    }
  }

  def getFromTrialId(trialId: Int): Validation[String, Option[ResponseAdaptiveRandomization]] = {
    database withSession {
      generateRandomizationMethodFromDatabaseEntry(queryRandomizationMethodFromTrialId(trialId).list)
    }
  }

  private def generateRandomizationMethodFromDatabaseEntry(resultList: List[(Option[Int], Int, Array[Byte],String)]): Validation[String, Option[ResponseAdaptiveRandomization]] = {
    if (resultList.isEmpty) Success(None)
    else if (resultList.size == 1) {
      val rm = resultList(0)
      if (rm._4 == classOf[ResponseAdaptiveRandomization].getName) {
        val responseAdaptiveMethods = queryAdaptiveRandomizationFromId(rm._1.get).list
        if(responseAdaptiveMethods.size !=1){
          Failure("Duplicated database entry or id not found")
        } else {
          val method = responseAdaptiveMethods.head
          val adaptiveRandomization = new ResponseAdaptiveRandomization(rm._1.get, method._2, method._4, method._5, method._6)(deserializeRandomGenerator(rm._3))
          Success(Some(adaptiveRandomization))
        }
      } else {
        Failure("Wrong plugin")
      }
    } else Failure("Duplicated database entry")
  }


  def update(randomizationMethod: ResponseAdaptiveRandomization): Validation[String, ResponseAdaptiveRandomization] = {
    database withSession {
      threadLocalSession withTransaction {

      }
    }
    get(randomizationMethod.id).toEither match {
      case Left(x) => Failure(x)
      case Right(None) => Failure("Method not found")
      case Right(Some(randomizationMethod)) => Success(randomizationMethod)
    }
  }

  def delete(randomizationMethod: ResponseAdaptiveRandomization) {
    database withSession {
      queryAdaptiveRandomizationFromId(randomizationMethod.id).mutate {
        r =>
          r.delete()
      }

      queryRandomizationMethodFromId(randomizationMethod.id).mutate {
        r =>
          r.delete()
      }

    }

  }

}
