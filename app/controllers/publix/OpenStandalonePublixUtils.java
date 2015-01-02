package controllers.publix;

import models.StudyModel;
import models.workers.OpenStandaloneWorker;
import models.workers.Worker;
import play.mvc.Http.Cookie;
import exceptions.ForbiddenPublixException;
import exceptions.PublixException;

/**
 * Special PublixUtils for OpenStandalonePublix
 * 
 * @author Kristian Lange
 */
public class OpenStandalonePublixUtils extends PublixUtils<OpenStandaloneWorker> {

	private OpenStandaloneErrorMessages errorMessages;

	public OpenStandalonePublixUtils(OpenStandaloneErrorMessages errorMessages) {
		super(errorMessages);
		this.errorMessages = errorMessages;
	}

	@Override
	public OpenStandaloneWorker retrieveTypedWorker(String workerIdStr)
			throws PublixException {
		Worker worker = retrieveWorker(workerIdStr);
		if (!(worker instanceof OpenStandaloneWorker)) {
			throw new ForbiddenPublixException(
					errorMessages.workerNotCorrectType(worker.getId()));
		}
		return (OpenStandaloneWorker) worker;
	}

	@Override
	public void checkWorkerAllowedToStartStudy(OpenStandaloneWorker worker,
			StudyModel study) throws ForbiddenPublixException {
		// no constrictions
		return;
	}

	@Override
	public void checkWorkerAllowedToDoStudy(OpenStandaloneWorker worker,
			StudyModel study) throws ForbiddenPublixException {
		// Standalone workers can't repeat the same study
		if (finishedStudyAlready(worker, study)) {
			throw new ForbiddenPublixException(
					PublixErrorMessages.STUDY_CAN_BE_DONE_ONLY_ONCE);
		}
	}

	public void checkAllowedToDoStudy(StudyModel study)
			throws ForbiddenPublixException {
		Cookie cookie = Publix.request().cookie(
				OpenStandalonePublix.COOKIE);
		if (cookie != null) {
			String[] studyIdArray = cookie.value().split(",");
			for (String idStr : studyIdArray) {
				if (study.getId().toString().equals(idStr)) {
					throw new ForbiddenPublixException(
							PublixErrorMessages.STUDY_CAN_BE_DONE_ONLY_ONCE);
				}
			}
		}
	}

	public void addStudyToCookie(StudyModel study) {
		Cookie cookie = Publix.request().cookie(
				OpenStandalonePublix.COOKIE);
		String value;
		if (cookie != null) {
			value = cookie.value() + "," + study.getId();
		} else {
			value = study.getId().toString();
		}
		Publix.response().setCookie(
				OpenStandalonePublix.COOKIE, value);
	}

}