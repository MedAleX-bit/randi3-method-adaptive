package org.randi3.randomization


import org.randi3.randomization.configuration._
import org.randi3.dao.ResponseAdaptiveRandomizationDao
import org.randi3.model._
import org.randi3.model.criterion.Criterion
import org.randi3.model.criterion.constraint.Constraint
import org.scalaquery.ql._
import org.scalaquery.ql.extended.ExtendedProfile
import org.scalaquery.session.Database
import scalaz._

import org.apache.commons.math3.random._

import org.randi3.schema.{LiquibaseUtil, AdaptiveRandomizationSchema}
import org.randi3.utility.{I18NHelper, I18NRandomization, AbstractSecurityUtil}

class ResponseAdaptiveRandomizationPlugin(database: Database, driver: ExtendedProfile, securityUtil: AbstractSecurityUtil) extends RandomizationMethodPlugin(database, driver, securityUtil) {


  private val i18n = new I18NRandomization(I18NHelper.getLocalizationMap("blockRandomizationM", getClass.getClassLoader), securityUtil)

  val schema = new AdaptiveRandomizationSchema(driver)
  import schema._


  val name = classOf[ResponseAdaptiveRandomization].getName

  def i18nName = i18n.text("name")

  def description = i18n.text("description")

  val canBeUsedWithStratification = true

  private val adaptiveRandomizationDao = new ResponseAdaptiveRandomizationDao(database, driver)

  private def initialCountBallsType = new IntegerConfigurationType(name = i18n.text("initialCountBalls"), description = i18n.text("initialCountBallsDesc"))
  private def countBallsResponseSuccessType = new IntegerConfigurationType(name = i18n.text("countBallsResponseSuccessType"), description = i18n.text("countBallsResponseSuccessTypeDesc"))
  private def countBallsResponseFailureType = new IntegerConfigurationType(name = i18n.text("countBallsResponseFailure"), description = i18n.text("countBallsResponseFailureDesc"))


  def randomizationConfigurationOptions(): (List[ConfigurationType[Any]], Map[String, List[Criterion[_ <: Any, Constraint[_ <: Any]]]]) = {
    (List(initialCountBallsType, countBallsResponseSuccessType, countBallsResponseFailureType), Map(("Response", List(ResponseAdaptiveRandomization.Criterion))))
  }

  def getRandomizationConfigurations(id: Int): List[ConfigurationProperty[Any]] = {
    val method = adaptiveRandomizationDao.get(id).toOption.getOrElse(return Nil).getOrElse(return Nil)
    List(new ConfigurationProperty[Any](initialCountBallsType, method.asInstanceOf[ResponseAdaptiveRandomization].initialCountBalls),
      new ConfigurationProperty[Any](countBallsResponseSuccessType, method.asInstanceOf[ResponseAdaptiveRandomization].countBallsResponseSuccess),
      new ConfigurationProperty[Any](countBallsResponseSuccessType, method.asInstanceOf[ResponseAdaptiveRandomization].countBallsResponseSuccess)
    )
  }

  def randomizationMethod(random: RandomGenerator, trial: Trial, configuration: List[ConfigurationProperty[Any]]): Validation[String, RandomizationMethod] = {
    if (configuration.isEmpty) Failure(i18n.text("configurationNotSet"))
    else {
      val initialCountBalls = configuration.find(config => config.configurationType == initialCountBallsType)
      val countsPerSuccess = configuration.find(config => config.configurationType == countBallsResponseSuccessType)
      val countsPerFailure = configuration.find(config => config.configurationType == countBallsResponseFailureType)

       if(initialCountBalls.isEmpty){
         Failure (i18n.text("initial count per balls not set"))
       } else
       if(countsPerSuccess.isEmpty){
        Failure (i18n.text("balls per success not set"))
      }  else
       if(countsPerFailure.isEmpty){
        Failure (i18n.text("balls per failure balls not set"))
      } else  {

         Success(new ResponseAdaptiveRandomization(initialCountBalls = initialCountBalls.get.value.asInstanceOf[Int],
           countBallsResponseSuccess = countsPerSuccess.get.value.asInstanceOf[Int],
           countBallsResponseFailure = countsPerFailure.get.value.asInstanceOf[Int])(random = random))
       }
    }



  }

  def databaseTables(): Option[DDL] = {
    Some(getDatabaseTables)
  }

  def updateDatabase() {
    LiquibaseUtil.updateDatabase(database, "db/db.changelog-master-adaptive.xml", this.getClass.getClassLoader)
  }

  def create(randomizationMethod: RandomizationMethod, trialId: Int): Validation[String, Int] = {
    adaptiveRandomizationDao.create(randomizationMethod.asInstanceOf[ResponseAdaptiveRandomization], trialId)
  }

  def get(id: Int): Validation[String, Option[RandomizationMethod]] = {
    adaptiveRandomizationDao.get(id)
  }

  def getFromTrialId(trialId: Int): Validation[String, Option[RandomizationMethod]] = {
    adaptiveRandomizationDao.getFromTrialId(trialId)
  }

  def update(randomizationMethod: RandomizationMethod): Validation[String, RandomizationMethod] = {
    adaptiveRandomizationDao.update(randomizationMethod.asInstanceOf[ResponseAdaptiveRandomization])
  }

  def delete(randomizationMethod: RandomizationMethod) {
    adaptiveRandomizationDao.delete(randomizationMethod.asInstanceOf[ResponseAdaptiveRandomization])
  }

}
