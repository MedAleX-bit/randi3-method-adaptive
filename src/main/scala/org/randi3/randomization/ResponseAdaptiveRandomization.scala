package org.randi3.randomization

import org.randi3.model.{TrialSubject, TreatmentArm, Trial}

import org.apache.commons.math3.random.RandomGenerator
import scala.collection.mutable.{ListBuffer, HashMap}
import scala.collection.mutable
import org.randi3.model.criterion.OrdinalCriterion


case class ResponseAdaptiveRandomization(id: Int = Int.MinValue, version: Int = 0, initialCountBalls: Int, countBallsResponseSuccess: Int, countBallsResponseFailure: Int)(val random: RandomGenerator) extends RandomizationMethod {

  import ResponseAdaptiveRandomization._


  def randomize(trial: Trial, subject: TrialSubject): TreatmentArm = {

    val responseAdaptiveUrns = initializeUrns(trial)

    val stratum = subject.getStratum(trial.isStratifiedByTrialSite)

    val urn = responseAdaptiveUrns.get(stratum).getOrElse {
      val newUrn = new ListBuffer[TreatmentArm]
      trial.treatmentArms.foreach(arm => {
        for (i <- 0 until initialCountBalls)
          newUrn.append(arm)
      })
      responseAdaptiveUrns.put(stratum, newUrn.toList)
      newUrn.toList
    }

    if (!urn.isEmpty) {
      urn(random.nextInt(urn.size))
    } else {
      trial.treatmentArms(random.nextInt(trial.treatmentArms.size))
    }
  }


  private def initializeUrns(trial: Trial): HashMap[String, List[TreatmentArm]] = {
    val urns = new mutable.HashMap[String, ListBuffer[TreatmentArm]]()

    trial.treatmentArms.foreach(arm => {

      arm.subjects.foreach(subject => {

        val stratum = subject.getStratum(trial.isStratifiedByTrialSite)

        val urn = urns.get(stratum).getOrElse {
          val newUrn = new ListBuffer[TreatmentArm]
          trial.treatmentArms.foreach(arm => {
            for (i <- 0 until initialCountBalls)
              newUrn.append(arm)
          })
          urns.put(stratum, newUrn)
          newUrn
        }

        if (!subject.stages.isEmpty) {
          val responseValue = subject.stages.head._2.head.value

          val otherArms = trial.treatmentArms.filter(actArm => actArm.id != arm.id)
          if (responseValue == Success) {

            for (i <- 0 until countBallsResponseSuccess) {
              urn.append(arm)
            }

            val countBallsOtherArms = countBallsResponseFailure / (trial.treatmentArms.size)

            for (otherArm <- otherArms) {
              for (int <- 0 until countBallsOtherArms) {
                urn.append(otherArm)
              }
            }
          } else {
            for (i <- 0 until countBallsResponseFailure) {
              urn.append(arm)
            }

            val countBallsOtherArms = countBallsResponseSuccess / (trial.treatmentArms.size)

            for (otherArm <- otherArms) {
              for (int <- 0 until countBallsOtherArms) {
                urn.append(otherArm)
              }
            }
          }
        }
      })
    })
    urns.map(element => (element._1, element._2.toList))
  }

}


object ResponseAdaptiveRandomization {

  val Success = "Success"
  val Failure = "Failure"

  val Criterion = OrdinalCriterion(name = "Response", description = "The response criterion", values = Set(Success, Failure), inclusionConstraint = None, strata = List()).toOption.get

}