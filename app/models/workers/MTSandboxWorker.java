package models.workers;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Model and DB entity of the concrete worker who originates from the MTurk
 * Sandbox.
 * 
 * @author Kristian Lange
 */
@Entity
@DiscriminatorValue(MTSandboxWorker.WORKER_TYPE)
public class MTSandboxWorker extends MTWorker {

	public static final String WORKER_TYPE = "MTSandbox";
	public static final String UI_WORKER_TYPE = "MTurk Sandbox";

	public MTSandboxWorker() {
	}

	@JsonCreator
	public MTSandboxWorker(String mtWorkerId) {
		super(mtWorkerId);
	}

	public String getUIWorkerType() {
		return UI_WORKER_TYPE;
	}

}
