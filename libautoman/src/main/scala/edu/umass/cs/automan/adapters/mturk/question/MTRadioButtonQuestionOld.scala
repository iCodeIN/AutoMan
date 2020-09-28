package edu.umass.cs.automan.adapters.mturk.question

import java.util.{Date, UUID}

import edu.umass.cs.automan.adapters.mturk.mock.RadioButtonMockResponse
import edu.umass.cs.automan.adapters.mturk.policy.aggregation.MTurkMinimumSpawnPolicy
import edu.umass.cs.automan.core.logging._
import edu.umass.cs.automan.core.question.{Dimension, RadioButtonQuestion}
import edu.umass.cs.automan.core.util.Utilities
import java.security.MessageDigest

import org.apache.commons.codec.binary.Hex

import scala.xml.{Elem, Node, NodeSeq}

class MTRadioButtonQuestionOld(sandbox: Boolean) extends RadioButtonQuestion with MTurkQuestion {
  type QuestionOptionType = MTQuestionOption
  override type A = RadioButtonQuestion#A // need this to be an array

  private val _action = if (sandbox) {
    "https://workersandbox.mturk.com/mturk/externalSubmit"
  } else {
    "https://www.mturk.com/mturk/externalSubmit"
  }

  // public API
  def memo_hash: String = {
    val md = MessageDigest.getInstance("md5")
    new String(Hex.encodeHex(md.digest(toXML(randomize = false).toString().getBytes)))
  }
  override def randomized_options: List[QuestionOptionType] = Utilities.randomPermute(options)
  override def description: String = _description match { case Some(d) => d; case None => this.title }
//  override def group_id: String = _title match { case Some(t) => t; case None => this.id.toString }
  override def group_id: String = title

  // private API
  _minimum_spawn_policy = MTurkMinimumSpawnPolicy
  private var _iframe_height = 450
  private var _layout: Option[scala.xml.Node] = None


  override def toMockResponse(question_id: UUID, response_time: Date, a: A, worker_id: UUID) : RadioButtonMockResponse = {
    RadioButtonMockResponse(question_id, response_time, a, worker_id)
  }
  override protected[mturk] def fromXML(x: scala.xml.Node) : A = { // RadioButtonQuestion#A
    // There should only be a SINGLE answer here, like this:
    //    <Answer>
    //      <QuestionIdentifier>721be9fc-c867-42ce-8acd-829e64ae62dd</QuestionIdentifier>
    //      <SelectionIdentifier>count</SelectionIdentifier>
    //    </Answer>
    DebugLog("MTRadioButtonQuestion: fromXML:\n" + x.toString,LogLevelDebug(),LogType.ADAPTER,id)

    Symbol((x \\ "Answer" \\ "FreeText").text) // todo separate fromSurveyXML?
    //Symbol((x \\ "Answer" \\ "SelectionIdentifier").text)
  }
  // TODO: random checkbox fill
  override protected[mturk] def toXML(randomize: Boolean): scala.xml.Node = {
    //<HTMLQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2011-11-11/HTMLQuestion.xsd">
    //options = options.map(MTQuestionOption(Symbol(UUID.randomUUID().toString), _, ""))
    <div>
      { toQuestionXML(randomize) }
    </div>
    //</HTMLQuestion>
//    <QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
//      { XMLBody(randomize) }
//    </QuestionForm>
  }

  def renderQuestion(dimension: Dimension) : scala.xml.Node = {
    val idname = s"dimension_${ dimension.id.toString.drop(1) }"
    <p>
      <input type="text" class="dimension" id={ idname } name={ dimension.id.toString.drop(1) } />
    </p>
  }

  // [[id, text], [id, text] ] nested JS arrays
  // iterate over, index 0 and 1
  private def generateOptions(): String = {
    val toRet: StringBuilder = new StringBuilder("[")
    for(opt <- options) {
      toRet.append("['" + opt.question_id.toString().drop(1) + "','")
      toRet.append(opt.question_text + "']")
    }
    toRet.append("]")
    toRet.toString()
  }
  //const form = document.createElement('crowd-form');
  // shuffle(options); on 102
//  <crowd-form>
//    <crowd-radio-group id ="radio">
//
//    </crowd-radio-group>
//  </crowd-form>
  //<![CDATA[
  //<!DOCTYPE html>

//  optTexts.forEach((option, index) => {
//    var form = document.getElementById('radio');
//    var input = document.createElement('crowd-radio-group');
//    input.name = 'option_' + option;
//    input.value = optIDs[index];
//    var optText = document.createTextNode(option);
//    input.appendChild(optText);
//    form.appendChild(input);
//  });
//}

  //<title>Please fill out this survey</title>
//  def html() = {
//    String.format("<!DOCTYPE html>%n") + {
////      <html>
////        <head>
////          <title>Please fill out this survey</title>
////          <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
////          <script>{ jsFunctions }</script>
////          {
////          _layout match {
////            case Some(layout) => layout
////            case None => NodeSeq.Empty
////          }
////          }
////          <script src="https://assets.crowd.aws/crowd-html-elements.js"></script>
////        </head>
////        <body onload="startup()">
////          <div id="wrapper">
////            <div id="hit_content">
////              <crowd-form name="mturk_form" method="post" id="mturk_form" action={_action}>
//              <div>
//                <input type="hidden" value={id.toString} name="question_id" id="question_id"/>
//                <input type="hidden" value="" name="assignmentId" id="assignmentId"/>
//                {
//                _image_url match {
//                  case Some(url) => <p><img id="question_image" src={ url }/></p>
//                  case None => NodeSeq.Empty
//                }
//                }
//                {
//                _text match {
//                  case Some(text) => <p>{ text }</p>
//                  case None => NodeSeq.Empty
//                }
//                }
//                { dimensions.map(renderQuestion) }
//              </div>
//
////              </crowd-form>
////            </div>
////          </div>
////        </body>
////      </html>
//    }.toString
//  }

  /**
    * Helper function to convert question into XML Question
    * Not usually called directly
    *
    * @param randomize Randomize option order?
    * @return XML
    */
  override protected[mturk] def toQuestionXML(randomize: Boolean): Seq[Node] = {
    Seq(
      toSurveyXML(randomize)//,
    //<FrameHeight>{ _iframe_height.toString }</FrameHeight>
    )
  }//<HTMLContent>//</HTMLContent>,

  override protected[mturk] def toSurveyXML(randomize: Boolean): Node = {
//      <input type="hidden" value={id.toString} name="question_id" id="question_id"/>
//        <input type="hidden" value="" name="assignmentId" id="assignmentId"/>
    <div id={id.toString} class="question">
      {
      _image_url match {
        case Some(url) => <p><img id="question_image" src={ url }/></p>
        case None => NodeSeq.Empty
      }
      }
      {
      _text match {
        case Some(text) => <p>{ text }</p>
        case None => NodeSeq.Empty
      }
      }
      <div id={s"opts_${id.toString}"}>
        {options.map(_.toSurveyXML(id))}
      </div>
    </div>
//    <crowd-radio-group>
//      <input type="hidden" value={id.toString} name="question_id" id="question_id"/>
//      <input type="hidden" value="" name="assignmentId" id="assignmentId"/>
//      <div>
//        {
//        _image_url match {
//          case Some(url) => <p><img id="question_image" src={ url }/></p>
//          case None => NodeSeq.Empty
//        }
//        }
//        {
//        _text match {
//          case Some(text) => <p>{ text }</p>
//          case None => NodeSeq.Empty
//        }
//        }
//      </div>
//      { options.map(optToXML(_)) }
//    </crowd-radio-group>
  }
  //scala.xml.PCData(html())
  //<HTMLContent>
  //{ scala.xml.PCData(html()) }
  //</HTMLContent>
  //<FrameHeight>{ _iframe_height.toString }</FrameHeight>


//  /**
//    * Converts a single option to a RB XML
//    * @param option the option to convert
//    * @return an XML node
//    */
    // all share a name
    // todo bad because no longer relying on QO toXML
//  private def optToXML(option: QuestionOptionType): Node = {
//      //<crowd-radio-button name={id.toString.drop(1)} value={option.question_id.toString().drop(1)}>{option.question_text}</crowd-radio-button>
//      <div>
//          <input type="radio" id={option.question_id.toString().drop(1)} name={id.toString} value={option.question_id.toString().drop(1)} required="required"/>
//          <label for={option.question_id.toString().drop(1)}>
//            {option.question_text}
//          </label>
//      </div>
//  }
//    <Question>
//      <QuestionIdentifier>{ if (randomize) id_string else "" }</QuestionIdentifier>
//      <IsRequired>true</IsRequired>
//      <QuestionContent>
//        {
//        _image_url match {
//          case Some(url) => {
//            <Binary>
//              <MimeType>
//                <Type>image</Type>
//                <SubType>png</SubType>
//              </MimeType>
//              <DataURL>{ url }</DataURL>
//              <AltText>{ image_alt_text }</AltText>
//            </Binary>
//          }
//          case None => {}
//        }
//        }
//        {
//        // if formatted content is specified, use that instead of text field
//        _formatted_content match {
//          case Some(x) => <FormattedContent>{ scala.xml.PCData(x.toString) }</FormattedContent>
//          case None => <Text>{ text }</Text>
//        }
//        }
//      </QuestionContent>
//      <AnswerSpecification>
//        <SelectionAnswer>
//          <StyleSuggestion>radiobutton</StyleSuggestion>
//          <Selections>{ if(randomize) {
//              <HTMLContent>
//
//                { scala.xml.PCData(html()) }
//
//              </HTMLContent>
//            }
//            //randomized_options.map { _.toXML }
//          } else {
//            options.map { _.toXML }
//          }
//          </Selections>
//        </SelectionAnswer>
//      </AnswerSpecification>
//    </Question>

  //Seq(
  //      <Question>
  //        <QuestionIdentifier>{ if (randomize) id_string else "" }</QuestionIdentifier>
  //        <IsRequired>true</IsRequired>
  //        <QuestionContent>
  //          {
  //          _image_url match {
  //            case Some(url) => {
  //              <Binary>
  //                <MimeType>
  //                  <Type>image</Type>
  //                  <SubType>png</SubType>
  //                </MimeType>
  //                <DataURL>{ url }</DataURL>
  //                <AltText>{ image_alt_text }</AltText>
  //              </Binary>
  //            }
  //            case None => {}
  //          }
  //          }
  //          {
  //          // if formatted content is specified, use that instead of text field
  //          _formatted_content match {
  //            case Some(x) => <FormattedContent>{ scala.xml.PCData(x.toString()) }</FormattedContent>
  //            case None => <Text>{ text }</Text>
  //          }
  //          }
  //        </QuestionContent>
  //        <AnswerSpecification>
  //          <SelectionAnswer>
  //            <StyleSuggestion>radiobutton</StyleSuggestion>
  //            <Selections>{ if(randomize) randomized_options.map { _.toXML } else options.map { _.toXML } }</Selections>
  //          </SelectionAnswer>
  //        </AnswerSpecification>
  //      </Question>
  //)
}