package org.randi3.utility

import org.randi3.dao._
import org.randi3.schema.{LiquibaseUtil, AdaptiveRandomizationSchema}


object TestingEnvironmentAdaptive extends TestingEnvironment{


  val schemaBlock = new AdaptiveRandomizationSchema(driver)

  LiquibaseUtil.updateDatabase(database, "db/db.changelog-master-adaptive.xml", this.getClass.getClassLoader)

  lazy val adaptiveRandomizationDao = new ResponseAdaptiveRandomizationDao(database, driver)

}