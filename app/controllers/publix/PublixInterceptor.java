package controllers.publix;

import models.workers.ClosedStandaloneWorker;
import models.workers.JatosWorker;
import models.workers.MTSandboxWorker;
import models.workers.MTWorker;
import models.workers.OpenStandaloneWorker;
import models.workers.TesterWorker;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;

import com.fasterxml.jackson.core.JsonProcessingException;

import exceptions.BadRequestPublixException;
import exceptions.PublixException;

/**
 * Interceptor for Publix: it intercepts requests for JATOS' public API and
 * forwards them to one of the implementations of the API (all extend Publix).
 * Each implementation deals with different workers (e.g. workers from MechTurk,
 * tester workers).
 * 
 * When a study is started the implementation to use is determined by parameters
 * in the request's query string. Then the worker type is put into the session
 * and used in subsequent requests of the same study run.
 * 
 * 1. Requests coming from MechTurk or MechTurk Sandbox (MTWorker and
 * MTSandboxWorker) will be forwarded to MTPublix.<br>
 * 2. Requests coming from Jatos' UI (if clicked on show study/component) run
 * (JatosWorker) will be forwarded to JatosPublix.<br>
 * 3. Requests coming from a tester run (TesterWorker) will be forwarded to
 * TesterPublix.<br>
 * 4. Requests coming from a closed standalone run (limited to pre-created
 * ClosedStandaloneWorker) will be forwarded to ClosedStandalonePublix.<br>
 * 5. Requests coming from an open standalone run (unlimited to everyone with
 * the link) will be forwarded to OpenStandalonePublix.<br>
 * 
 * TODO: Move @Transactional out of controller and get rid of synchronisation
 * and JPA transaction handling
 * 
 * @author Kristian Lange
 */
public class PublixInterceptor extends Controller implements IPublix {

	public static final String WORKER_TYPE = "workerType";

	private static Object lock = new Object();

	private IPublix jatosPublix = new JatosPublix();
	private IPublix mtPublix = new MTPublix();
	private IPublix testerPublix = new TesterPublix();
	private IPublix closedStandalonePublix = new ClosedStandalonePublix();
	private IPublix openStandalonePublix = new OpenStandalonePublix();

	@Override
	@Transactional
	public Result startStudy(Long studyId) throws PublixException {
		synchronized (lock) {
			Result result = null;
			String workerType = getWorkerTypeFromQuery();
			// Put worker type into session so later Pulix calls of this study
			// run know it too
			session(WORKER_TYPE, workerType);
			switch (workerType) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				result = mtPublix.startStudy(studyId);
				break;
			case JatosWorker.WORKER_TYPE:
				result = jatosPublix.startStudy(studyId);
				break;
			case TesterWorker.WORKER_TYPE:
				result = testerPublix.startStudy(studyId);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				result = closedStandalonePublix.startStudy(studyId);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				result = openStandalonePublix.startStudy(studyId);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Promise<Result> startComponent(Long studyId, Long componentId)
			throws PublixException {
		synchronized (lock) {
			Promise<Result> promise = null;
			switch (getWorkerTypeFromSession()) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				promise = mtPublix.startComponent(studyId, componentId);
				break;
			case JatosWorker.WORKER_TYPE:
				promise = jatosPublix.startComponent(studyId, componentId);
				break;
			case TesterWorker.WORKER_TYPE:
				promise = testerPublix.startComponent(studyId, componentId);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				promise = closedStandalonePublix.startComponent(studyId,
						componentId);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				promise = openStandalonePublix.startComponent(studyId,
						componentId);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return promise;
		}
	}

	@Override
	@Transactional
	public Promise<Result> startComponentByPosition(Long studyId,
			Integer position) throws PublixException {
		// This method calls startComponent(). Therefore no synchronisation
		// and JPA transaction handling
		Promise<Result> promise = null;
		switch (getWorkerTypeFromSession()) {
		case MTWorker.WORKER_TYPE:
		case MTSandboxWorker.WORKER_TYPE:
			promise = mtPublix.startComponentByPosition(studyId, position);
			break;
		case JatosWorker.WORKER_TYPE:
			promise = jatosPublix.startComponentByPosition(studyId, position);
			break;
		case TesterWorker.WORKER_TYPE:
			promise = testerPublix.startComponentByPosition(studyId, position);
			break;
		case ClosedStandaloneWorker.WORKER_TYPE:
			promise = closedStandalonePublix.startComponentByPosition(studyId,
					position);
			break;
		case OpenStandaloneWorker.WORKER_TYPE:
			promise = openStandalonePublix.startComponentByPosition(studyId,
					position);
			break;
		default:
			throw new BadRequestPublixException(
					PublixErrorMessages.UNKNOWN_WORKER_TYPE);
		}
		return promise;
	}

	@Override
	@Transactional
	public Result startNextComponent(Long studyId) throws PublixException {
		synchronized (lock) {
			Result result = null;
			switch (getWorkerTypeFromSession()) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				result = mtPublix.startNextComponent(studyId);
				break;
			case JatosWorker.WORKER_TYPE:
				result = jatosPublix.startNextComponent(studyId);
				break;
			case TesterWorker.WORKER_TYPE:
				result = testerPublix.startNextComponent(studyId);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				result = closedStandalonePublix.startNextComponent(studyId);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				result = openStandalonePublix.startNextComponent(studyId);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result getStudyData(Long studyId) throws PublixException,
			JsonProcessingException {
		synchronized (lock) {
			Result result = null;
			switch (getWorkerTypeFromSession()) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				result = mtPublix.getStudyData(studyId);
				break;
			case JatosWorker.WORKER_TYPE:
				result = jatosPublix.getStudyData(studyId);
				break;
			case TesterWorker.WORKER_TYPE:
				result = testerPublix.getStudyData(studyId);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				result = closedStandalonePublix.getStudyData(studyId);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				result = openStandalonePublix.getStudyData(studyId);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result getComponentData(Long studyId, Long componentId)
			throws PublixException, JsonProcessingException {
		synchronized (lock) {
			Result result = null;
			switch (getWorkerTypeFromSession()) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				result = mtPublix.getComponentData(studyId, componentId);
				break;
			case JatosWorker.WORKER_TYPE:
				result = jatosPublix.getComponentData(studyId, componentId);
				break;
			case TesterWorker.WORKER_TYPE:
				result = testerPublix.getComponentData(studyId, componentId);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				result = closedStandalonePublix.getComponentData(studyId,
						componentId);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				result = openStandalonePublix.getComponentData(studyId,
						componentId);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result submitResultData(Long studyId, Long componentId)
			throws PublixException {
		synchronized (lock) {
			Result result = null;
			switch (getWorkerTypeFromSession()) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				result = mtPublix.submitResultData(studyId, componentId);
				break;
			case JatosWorker.WORKER_TYPE:
				result = jatosPublix.submitResultData(studyId, componentId);
				break;
			case TesterWorker.WORKER_TYPE:
				result = testerPublix.submitResultData(studyId, componentId);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				result = closedStandalonePublix.submitResultData(studyId,
						componentId);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				result = openStandalonePublix.submitResultData(studyId,
						componentId);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result finishComponent(Long studyId, Long componentId,
			Boolean successful, String errorMsg) throws PublixException {
		synchronized (lock) {
			Result result = null;
			switch (getWorkerTypeFromSession()) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				result = mtPublix.finishComponent(studyId, componentId,
						successful, errorMsg);
				break;
			case JatosWorker.WORKER_TYPE:
				result = jatosPublix.finishComponent(studyId, componentId,
						successful, errorMsg);
				break;
			case TesterWorker.WORKER_TYPE:
				result = testerPublix.finishComponent(studyId, componentId,
						successful, errorMsg);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				result = closedStandalonePublix.finishComponent(studyId,
						componentId, successful, errorMsg);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				result = openStandalonePublix.finishComponent(studyId,
						componentId, successful, errorMsg);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result abortStudy(Long studyId, String message)
			throws PublixException {
		synchronized (lock) {
			Result result = null;
			switch (getWorkerTypeFromSession()) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				result = mtPublix.abortStudy(studyId, message);
				break;
			case JatosWorker.WORKER_TYPE:
				result = jatosPublix.abortStudy(studyId, message);
				break;
			case TesterWorker.WORKER_TYPE:
				result = testerPublix.abortStudy(studyId, message);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				result = closedStandalonePublix.abortStudy(studyId, message);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				result = openStandalonePublix.abortStudy(studyId, message);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	@Transactional
	public Result finishStudy(Long studyId, Boolean successful, String errorMsg)
			throws PublixException {
		synchronized (lock) {
			Result result = null;
			switch (getWorkerTypeFromSession()) {
			case MTWorker.WORKER_TYPE:
			case MTSandboxWorker.WORKER_TYPE:
				result = mtPublix.finishStudy(studyId, successful, errorMsg);
				break;
			case JatosWorker.WORKER_TYPE:
				result = jatosPublix.finishStudy(studyId, successful, errorMsg);
				break;
			case TesterWorker.WORKER_TYPE:
				result = testerPublix
						.finishStudy(studyId, successful, errorMsg);
				break;
			case ClosedStandaloneWorker.WORKER_TYPE:
				result = closedStandalonePublix.finishStudy(studyId,
						successful, errorMsg);
				break;
			case OpenStandaloneWorker.WORKER_TYPE:
				result = openStandalonePublix.finishStudy(studyId, successful,
						errorMsg);
				break;
			default:
				throw new BadRequestPublixException(
						PublixErrorMessages.UNKNOWN_WORKER_TYPE);
			}
			session().remove(WORKER_TYPE);
			JPA.em().flush();
			JPA.em().getTransaction().commit();
			JPA.em().getTransaction().begin();
			return result;
		}
	}

	@Override
	public Result logError(Long studyId, Long componentId)
			throws BadRequestPublixException {
		switch (getWorkerTypeFromSession()) {
		case MTWorker.WORKER_TYPE:
		case MTSandboxWorker.WORKER_TYPE:
			return mtPublix.logError(studyId, componentId);
		case JatosWorker.WORKER_TYPE:
			return jatosPublix.logError(studyId, componentId);
		case TesterWorker.WORKER_TYPE:
			return testerPublix.logError(studyId, componentId);
		case ClosedStandaloneWorker.WORKER_TYPE:
			return closedStandalonePublix.logError(studyId, componentId);
		case OpenStandaloneWorker.WORKER_TYPE:
			return openStandalonePublix.logError(studyId, componentId);
		default:
			throw new BadRequestPublixException(
					PublixErrorMessages.UNKNOWN_WORKER_TYPE);
		}
	}

	/**
	 * Checks session which type of worker is doing the study. Returns a String
	 * specifying the worker type.
	 */
	private String getWorkerTypeFromSession() throws BadRequestPublixException {
		// Check if we have workerType in session
		String workerType = session(WORKER_TYPE);
		if (workerType != null) {
			return workerType;
		}
		throw new BadRequestPublixException(
				PublixErrorMessages.UNKNOWN_WORKER_TYPE);
	}

	/**
	 * Checks the request's query string which type of worker is doing the
	 * study. Returns a String specifying the worker type. Before a study is
	 * started the worker type is specified via a parameter in the query string.
	 */
	private String getWorkerTypeFromQuery() throws BadRequestPublixException {
		// Check for JATOS worker
		String jatosWorkerId = Publix
				.getQueryString(JatosPublix.JATOS_WORKER_ID);
		if (jatosWorkerId != null) {
			return JatosWorker.WORKER_TYPE;
		}
		// Check for MT worker and MT Sandbox worker
		String mtWorkerId = Publix.getQueryString(MTPublix.MT_WORKER_ID);
		if (mtWorkerId != null) {
			String turkSubmitTo = request().getQueryString(
					MTPublix.TURK_SUBMIT_TO);
			if (turkSubmitTo != null
					&& turkSubmitTo.toLowerCase().contains(MTPublix.SANDBOX)) {
				return MTSandboxWorker.WORKER_TYPE;
			} else {
				return MTWorker.WORKER_TYPE;
			}
		}
		// Check for tester worker
		String testerId = Publix.getQueryString(TesterPublix.TESTER_ID);
		if (testerId != null) {
			return TesterWorker.WORKER_TYPE;
		}
		// Check for closed standalone worker
		String closedStandaloneWorkerId = Publix
				.getQueryString(ClosedStandalonePublix.CLOSEDSTANDALONE_WORKER_ID);
		if (closedStandaloneWorkerId != null) {
			return ClosedStandaloneWorker.WORKER_TYPE;
		}
		// Check for open standalone worker
		String openStandalone = Publix
				.getQueryString(OpenStandalonePublix.OPENSTANDALONE);
		if (openStandalone != null) {
			return OpenStandaloneWorker.WORKER_TYPE;
		}
		throw new BadRequestPublixException(
				PublixErrorMessages.UNKNOWN_WORKER_TYPE);
	}

}