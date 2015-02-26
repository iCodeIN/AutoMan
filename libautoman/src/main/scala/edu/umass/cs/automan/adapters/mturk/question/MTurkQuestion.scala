package edu.umass.cs.automan.adapters.mturk.question

import edu.umass.cs.automan.core.scheduler.{BackendResult, Thunk}
import com.amazonaws.mturk.requester.{HIT, Assignment, QualificationRequirement}

trait MTurkQuestion {
  type A

  protected var _description: String = ""
  protected var _qualified_workers = Map[String,Set[String]]() // (QualificationTypeId -> Set[worker_id])
  protected var _formatted_content: Option[scala.xml.NodeSeq] = None
  protected var _keywords = List[String]()
  protected var _qualifications = List[QualificationRequirement]()
  protected var _group_id: String

  def answer(a: Assignment): BackendResult[A]
  def description_=(d: String) { _description = d }
  def description: String = _description
  def formatted_content: scala.xml.NodeSeq = _formatted_content match {
    case Some(x) => x
    case None => scala.xml.NodeSeq.Empty
  }
  def formatted_content_=(x: scala.xml.NodeSeq) { _formatted_content = Some(x) }
  def group_id_=(id: String) { _group_id = id }
  def group_id: String = _group_id
  def keywords_=(ks: List[String]) { _keywords = ks }
  def keywords: List[String] = _keywords
  def qualifications_=(qs: List[QualificationRequirement]) { _qualifications = qs }
  def qualifications: List[QualificationRequirement] = _qualifications
  def toXML(randomize: Boolean) : scala.xml.Node
}
