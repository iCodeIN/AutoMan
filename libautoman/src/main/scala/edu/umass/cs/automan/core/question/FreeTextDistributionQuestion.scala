package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.AutomanAdapter
import edu.umass.cs.automan.core.answer._
import edu.umass.cs.automan.core.info.QuestionType
import edu.umass.cs.automan.core.logging.Memo
import edu.umass.cs.automan.core.scheduler.Scheduler
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class FreeTextDistributionQuestion extends DistributionQuestion {
  type A = String
  type AA = Answers[A]
  type O = DistributionOutcome[A]
  type QuestionOptionType <: QuestionOption

  protected var _allow_empty: Boolean = false
  protected var _num_possibilities: BigInt = 1000
  protected var _pattern: Option[String] = None
  protected var _regex: Option[String] = None
  protected var _pattern_error_text: String = ""

  def allow_empty_pattern_=(ae: Boolean) { _allow_empty = ae }
  def allow_empty_pattern: Boolean = _allow_empty
  def num_possibilities: BigInt = _num_possibilities
  def pattern: String = _pattern match { case Some(p) => p; case None => ".*" }
  def pattern_=(p: String) { _pattern = Some(p) }
  def pattern_error_text: String = _pattern_error_text
  def pattern_error_text_=(p: String) { _pattern_error_text = p }

  override protected[automan] def getQuestionType = QuestionType.FreeTextDistributionQuestion
  override protected[automan] def getOutcome(adapter: AutomanAdapter, memo: Memo, poll_interval_in_s: Int) : O = {
    val scheduler = new Scheduler(this, adapter, memo)
    val f = Future{
      blocking {
        scheduler.run().asInstanceOf[AA]
      }
    }
    DistributionOutcome(f)
  }
  override protected [automan] def questionStartupHook(): Unit = {
    super.questionStartupHook()

    _pattern match {
      case Some(pattern) =>
        PictureClause(pattern, _allow_empty) match {
          case (regex, count) => {
            _regex = Some(regex)
            // the following odd calculation exists to prevent overflow
            // in MonteCarlo simulator; 1/1000 are sufficiently low odds
            _num_possibilities = if (count > 1000) 1000 else count
          }
        }
      case None => ()
    }
  }
}
