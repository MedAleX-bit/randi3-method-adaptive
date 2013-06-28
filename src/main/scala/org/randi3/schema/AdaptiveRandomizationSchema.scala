package org.randi3.schema

import org.scalaquery.ql._
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.{ExtendedTable => Table}
import org.scalaquery.ql.extended._


class AdaptiveRandomizationSchema(driver: ExtendedProfile) {

  val schema = new DatabaseSchema(driver)

  object AdaptiveRandomizations extends Table[(Int, Int, Option[Int], Int, Int, Int)]("AdaptiveRandomization") {
    def id = column[Int]("id", O PrimaryKey, O AutoInc)

    def version = column[Int]("Version", O NotNull)

    def randomizationMethodId = column[Option[Int]]("RandomizationMethodId")

    def initialCountBalls = column[Int]("InitialCountBalls", O NotNull)

    def ballsPerSuccess = column[Int]("BallsPerSuccess", O NotNull)

    def ballsPerFailure = column[Int]("BallsPerFailure", O NotNull)

    def * = id ~ version ~ randomizationMethodId ~ initialCountBalls ~ ballsPerSuccess ~ ballsPerFailure

    def noId = version ~ randomizationMethodId ~ initialCountBalls ~ ballsPerSuccess ~ ballsPerFailure

    def randomizationMethod = foreignKey("AdaptiveRandomizationFK_RandomizationMethod", randomizationMethodId, schema.RandomizationMethods)(_.id)
  }


  val getDatabaseTables: DDL = null

}
