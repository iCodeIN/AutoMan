package org.automanlang.adapters.mturk

import java.util.{Date, Locale}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials, AWSCredentialsProvider, AWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.mturk.{AmazonMTurk, AmazonMTurkClientBuilder}
import org.automanlang.adapters.mturk.worker.WorkerRunnable.RetryState
import org.automanlang.adapters.mturk.worker.{MTurkMethods, TurkWorker, WorkerRunnable}
import org.automanlang.adapters.mturk.logging.MTMemo
import org.automanlang.adapters.mturk.mock.{MockRequesterService, MockServiceState, MockSetup}
import org.automanlang.adapters.mturk.question._
import org.automanlang.core.logging.{DebugLog, LogLevelDebug, LogType}
import org.automanlang.core.question.Question
import org.automanlang.core.scheduler.{SchedulerState, Task}
import org.automanlang.core.AutomanAdapter

/* some fiddly classes to keep the AWS backend happy */
case class AutoManCredentialsProvider(awsAccessKeyId: String, awsSecretKey: String) extends AWSCredentialsProvider {
  override def getCredentials: AWSCredentials = AutoManCredentials(awsAccessKeyId, awsSecretKey)
  override def refresh(): Unit = ()
}

case class AutoManCredentials(awsAccessKeyId: String, awsSecretKey: String) extends AWSCredentials {
  override def getAWSAccessKeyId: String = awsAccessKeyId
  override def getAWSSecretKey: String = awsSecretKey
}

object MTurkAdapter {
  def apply(init: MTurkAdapter => Unit) : MTurkAdapter = {
    val mta = new MTurkAdapter
    init(mta)                     // assign values to fields in anonymous constructor
    mta.init()                    // run superclass initializer with values from previous line
    mta.setup()                   // initializes MTurk SDK
    mta.backend_budget()          // asks MTurk for our current budget
    mta
  }
}

class MTurkAdapter extends AutomanAdapter {
  // these types provide MTurk implementations for
  // AutomanAdapter virtual methods
  override type CBQ     = MTCheckboxQuestion
  override type CBDQ    = MTCheckboxVectorQuestion
  override type MEQ     = MTMultiEstimationQuestion
  override type EQ      = MTEstimationQuestion
  override type FTQ     = MTFreeTextQuestion
  override type FTDQ    = MTFreeTextVectorQuestion
  override type RBQ     = MTRadioButtonQuestion
  override type RBDQ    = MTRadioButtonVectorQuestion
  override type MemoDB  = MTMemo

  private val SANDBOX_ENDPOINT = "mturk-requester-sandbox.us-east-1.amazonaws.com"
  private val PROD_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com"
  private val SIGNING_REGION = "us-east-1"

  private var _access_key_id: Option[String] = None
  private var _backend_update_frequency_ms : Int = 4500 // lower than 1 second is inadvisable
  private var _worker : Option[TurkWorker] = None
  private var _secret_access_key: Option[String] = None
  private var _endpoint : EndpointConfiguration = new EndpointConfiguration(SANDBOX_ENDPOINT, SIGNING_REGION)
  private var _service : Option[AmazonMTurk] = None
  private var _use_mock: Option[MockSetup] = None

  // user-visible getters and setters
  def access_key_id: String = _access_key_id match { case Some(id) => id; case None => "" }
  def access_key_id_=(id: String) { _access_key_id = Some(id) }
  def backend_update_frequency_ms = _backend_update_frequency_ms
  def backend_update_frequency_ms_=(ms: Int) { _backend_update_frequency_ms = ms }
  def locale: Locale = _locale
  def locale_=(l: Locale) { _locale = l }
  def use_mock: MockSetup = _use_mock match { case Some(ms) => ms; case None => ??? }
  def use_mock_=(mock_setup: MockSetup) { _use_mock = Some(mock_setup) }
  def sandbox_mode = {
    _endpoint.getServiceEndpoint != PROD_ENDPOINT
  }
  def sandbox_mode_=(b: Boolean) {
    b match {
      case true => _endpoint = new EndpointConfiguration(SANDBOX_ENDPOINT, SIGNING_REGION)
      case false => _endpoint = new EndpointConfiguration(PROD_ENDPOINT, SIGNING_REGION)
    }
  }
  def secret_access_key: String = _secret_access_key match { case Some(s) => s; case None => "" }
  def secret_access_key_=(s: String) { _secret_access_key = Some(s) }

  protected def CBQFactory()  = new MTCheckboxQuestion
  protected def CBDQFactory() = new MTCheckboxVectorQuestion
  protected def MEQFactory()  = new MTMultiEstimationQuestion(sandbox_mode)
  protected def EQFactory()   = new MTEstimationQuestion
  protected def FTQFactory()  = new MTFreeTextQuestion
  protected def FTDQFactory() = new MTFreeTextVectorQuestion
  protected def RBQFactory()  = new MTRadioButtonQuestion()
  protected def RBDQFactory() = new MTRadioButtonVectorQuestion

  def Option(id: Symbol, text: String) = new MTQuestionOption(id, text, "")
  def Option(id: Symbol, text: String, image_url: String) = new MTQuestionOption(id, text, image_url)

  protected[automanlang] def accept(ts: List[Task]) = {
    assert(ts.forall { t =>
        t.state == SchedulerState.ANSWERED ||
        t.state == SchedulerState.DUPLICATE
      }
    )
    run_if_initialized((p: TurkWorker) => p.accept(ts))
  }
  protected[automanlang] def backend_budget() = run_if_initialized((p: TurkWorker) => p.backend_budget)
  protected[automanlang] def cancel(ts: List[Task], toState: SchedulerState.Value) = {
    assert(ts.nonEmpty)
    assert(
      ts.forall { t =>
        t.state != SchedulerState.CANCELLED &&
          t.state != SchedulerState.ACCEPTED &&
          t.state != SchedulerState.REJECTED
      }
    )
    run_if_initialized((p: TurkWorker) => p.cancel(ts, toState))
  }
  protected[automanlang] def post(ts: List[Task], exclude_worker_ids: List[String]) = {
    assert(ts.forall(_.state == SchedulerState.READY))
    run_if_initialized((p: TurkWorker) => p.post(ts, exclude_worker_ids))
  }
  protected[automanlang] def reject(ts_reasons: List[(Task, String)]) = {
    ts_reasons.foreach{ case (t,_) => assert(t.state == SchedulerState.ANSWERED, "State during reject is: " + t.state) }
    run_if_initialized((p: TurkWorker) => p.reject(ts_reasons))
  }
  protected[automanlang] def retrieve(ts: List[Task], current_time: Date) = {
    assert(ts.forall(_.state == SchedulerState.RUNNING))
    run_if_initialized((p: TurkWorker) => p.retrieve(ts, current_time))
  }
  protected[automanlang] def requesterService = _service
  override protected[automanlang] def question_startup_hook(q: Question, t: Date): Unit = {
    super.question_startup_hook(q, t)
    // do simulation-specific setup
    _use_mock match {
      case Some(mock_setup) =>
        // register question with MockRequesterService
        _service.get.asInstanceOf[MockRequesterService].registerQuestion(q, t)
        // shorten scheduler sleep interval
        q.update_frequency_ms = 0
      case _ => ()
    }
  }

  // exception helper function
  private def run_if_initialized[U](f: TurkWorker => U) : U = {
    _worker match {
      case Some(p) => f(p)
      case None => {
        throw MTurkAdapterNotInitialized("MTurkAdapter must be initialized before attempting to communicate.")
      }
    }
  }

  // credentials provider helper-- needed to set static credentials
  // for MTurk backend
  private def credentials: AutoManCredentialsProvider = {
    AutoManCredentialsProvider(access_key_id, secret_access_key)
  }

  // initialization routines
  private def setup() {
    val rs = _use_mock match {
      case Some(mock_setup) =>
        val mss = MockServiceState(
          mock_setup.balance.bigDecimal,
          Map.empty,
          Map.empty,
          Map.empty,
          Map.empty,
          Map.empty,
          Map.empty,
          List.empty
        )
        new MockRequesterService(mss)
      case None => {
        val builder: AmazonMTurkClientBuilder = AmazonMTurkClientBuilder.standard
        builder.setEndpointConfiguration(_endpoint)
        builder.setCredentials(credentials)
        builder.build()
      }
    }
    val pool = _use_mock match {
      case Some(mock_setup) =>
        new TurkWorker(rs, 0, Some(rs.asInstanceOf[MockRequesterService]), _memoizer)
      case None =>
        new TurkWorker(rs, _backend_update_frequency_ms, None, _memoizer)
    }
    _service = Some(rs)
    _worker = Some(pool)
  }

  private def toClientConfig = {
    val _creds = new AWSStaticCredentialsProvider(
                  new BasicAWSCredentials(_access_key_id.getOrElse(""), _secret_access_key.getOrElse(""))
                 )
    val _config: AmazonMTurkClientBuilder = AmazonMTurkClientBuilder.standard
    _config.setEndpointConfiguration(_endpoint)
    _config.setCredentials(_creds)
    _config.build()
   }

  override protected[automanlang] def close(): Unit = {
    super.close()
    _worker match {
      case Some(p) =>
        // cleanup qualifications
        p.cleanup_qualifications()

        // shutdown backend
        p.shutdown()
      case None => ()
    }
  }
  override protected def MemoDBFactory() : MemoDB = {
    DebugLog("Initializing memo DB \"" + _database_path + "\" with MTurk extensions.", LogLevelDebug(), LogType.ADAPTER, null)
    new MTMemo(_log_config, _database_path, _in_mem_db)
  }
  protected[automanlang] def getAllHITs : Array[AnyRef] = { //TODO: fix types
    _service match {
      case Some(rs) =>
        val timeoutState = new RetryState(_backend_update_frequency_ms)
        WorkerRunnable.turkRetry(() => MTurkMethods.mturk_searchAllHITs(rs).getHITs.toArray(), timeoutState)
      case None => Array[AnyRef]()
    }
  }
}
