import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

import org.junit.Test;

import play.mvc.Content;
import play.mvc.Result;
import play.test.Helpers;

/**
 * 
 * Simple (JUnit) tests that can call all parts of a play app. If you are
 * interested in mocking a whole application, see the wiki for more details.
 * 
 */
public class ApplicationTest {

	@Test
	public void simpleCheck() {
		int a = 1 + 1;
		assertThat(a).isEqualTo(2);
	}

	@Test
	public void renderTemplate() {
		Content html = views.html.publix.index.render();
		assertThat(contentType(html)).isEqualTo("text/html");
		// assertThat(contentAsString(html)).contains("Your new application is ready.");
	}

	@Test
	public void callIndex() {
		Result result = callAction(controllers.routes.ref.Publix.index());
		assertThat(status(result)).isEqualTo(OK);
	}

	@Test
	public void callStartStudy() {
		running(fakeApplication(Helpers.inMemoryDatabase()), new Runnable() {
			@Override
			public void run() {
				Result result = callAction(controllers.routes.ref.Publix
						.startStudy(1));
				assertThat(status(result)).isEqualTo(OK);
				assertThat(charset(result)).isEqualTo("utf-8");
				assertThat(contentAsString(result)).contains("Hello Kiki");
			}
		});
	}
}
