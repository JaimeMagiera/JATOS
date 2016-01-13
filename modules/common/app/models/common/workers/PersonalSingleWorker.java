package models.common.workers;

import general.common.MessagesStrings;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import play.data.validation.ValidationError;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Model and DB entity of the concrete Worker if a study is run as Personal
 * Single Worker. This kind of worker is for an external run. The worker can run
 * the study only once. The worker is created by a JATOS user before the study
 * is started.
 * 
 * @author Kristian Lange
 */
@Entity
@DiscriminatorValue(PersonalSingleWorker.WORKER_TYPE)
public class PersonalSingleWorker extends Worker {

	public static final String WORKER_TYPE = "PersonalSingle";
	public static final String UI_WORKER_TYPE = "Personal Single";
	public static final String COMMENT = "comment";

	private String comment;

	public PersonalSingleWorker() {
	}

	@JsonCreator
	public PersonalSingleWorker(String comment) {
		this.comment = comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getComment() {
		return this.comment;
	}

	public String getWorkerType() {
		return WORKER_TYPE;
	}

	public String getUIWorkerType() {
		return UI_WORKER_TYPE;
	}

	@Override
	public String generateConfirmationCode() {
		return null;
	}

	@Override
	public List<ValidationError> validate() {
		List<ValidationError> errorList = new ArrayList<>();
		if (comment != null && !Jsoup.isValid(comment, Whitelist.none())) {
			errorList.add(new ValidationError(COMMENT,
					MessagesStrings.NO_HTML_ALLOWED));
		}
		return errorList.isEmpty() ? null : errorList;
	}

}
