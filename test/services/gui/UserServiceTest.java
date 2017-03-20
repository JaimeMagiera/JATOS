package services.gui;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;

import javax.inject.Inject;

import org.fest.assertions.Fail;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import controllers.gui.Authentication;
import daos.common.StudyDao;
import daos.common.UserDao;
import exceptions.gui.ForbiddenException;
import exceptions.gui.NotFoundException;
import general.TestHelper;
import general.common.MessagesStrings;
import general.common.RequestScope;
import models.common.Study;
import models.common.User;
import models.common.User.Role;
import models.gui.NewUserModel;
import play.ApplicationLoader;
import play.Environment;
import play.db.jpa.JPAApi;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

/**
 * Tests UserService
 * 
 * @author Kristian Lange
 */
public class UserServiceTest {

	private Injector injector;

	@Inject
	private TestHelper testHelper;

	@Inject
	private JPAApi jpaApi;

	@Inject
	private UserService userService;

	@Inject
	private UserDao userDao;

	@Inject
	private StudyDao studyDao;

	@Inject
	private StudyDao batchDao;

	@Before
	public void startApp() throws Exception {
		GuiceApplicationBuilder builder = new GuiceApplicationLoader()
				.builder(new ApplicationLoader.Context(Environment.simple()));
		injector = Guice.createInjector(builder.applicationModule());
		injector.injectMembers(this);
	}

	@After
	public void stopApp() throws Exception {
		// Clean up
		testHelper.removeAllStudies();
		testHelper.removeStudyAssetsRootDir();
	}

	@Test
	public void simpleCheck() {
		int a = 1 + 1;
		assertThat(a).isEqualTo(2);
	}

	/**
	 * Test UserService.authenticate(): check with admin user
	 */
	@Test
	public void checkAuthenticate() {
		jpaApi.withTransaction(() -> {
			assertThat(userService.authenticate(UserService.ADMIN_EMAIL,
					UserService.ADMIN_PASSWORD)).isTrue();
			assertThat(userService.authenticate(UserService.ADMIN_EMAIL,
					"wrongPassword")).isFalse();
		});
	}

	/**
	 * Test UserService.authenticate(): check with a different user than the
	 * admin user (shouldn't be necessary - just to be sure)
	 */
	@Test
	public void checkAuthenticateNotAdminUser() {
		testHelper.createAndPersistUser("bla@bla.org", "Bla Bla", "bla");

		jpaApi.withTransaction(() -> {
			assertThat(userService.authenticate("bla@bla.org", "bla")).isTrue();
			assertThat(userService.authenticate("bla@bla.org", "wrongPassword"))
					.isFalse();
		});

		testHelper.removeUser("bla@bla.org");
	}

	/**
	 * Test UserService.retrieveUser(): gets a user from the DB
	 */
	@Test
	public void checkRetrieveUser() {
		// Check retrieval of admin user
		jpaApi.withTransaction(() -> {
			User admin = testHelper.getAdmin();
			User user = null;
			try {
				user = userService.retrieveUser("admin");
			} catch (NotFoundException e) {
				Fail.fail();
			}
			assertThat(user).isEqualTo(admin);
		});

		// Unknown user should throw NotFoundException
		jpaApi.withTransaction(() -> {
			try {
				userService.retrieveUser("user-not-exist");
				Fail.fail();
			} catch (NotFoundException e) {
				assertThat(e.getMessage()).isEqualTo(
						MessagesStrings.userNotExist("user-not-exist"));
			}
		});
	}

	/**
	 * Test UserService.bindToUserAndPersist()
	 */
	@Test
	public void checkBindToUserAndPersist() {
		NewUserModel userModelBla = new NewUserModel();
		userModelBla.setEmail("bla@bla.org");
		userModelBla.setName("Bla Bla");
		userModelBla.setAdminPassword("nobodyCaresAtThisPoint");
		userModelBla.setPassword("blaPw");
		userModelBla.setPasswordRepeat("blaPw");
		userModelBla.setAdminRole(true);

		jpaApi.withTransaction(() -> {
			userService.bindToUserAndPersist(userModelBla);
		});

		// Check that the user is stored in the DB properly
		jpaApi.withTransaction(() -> {
			User u = userDao.findByEmail(userModelBla.getEmail());
			assertThat(u.getEmail()).isEqualTo(userModelBla.getEmail());
			assertThat(u.getName()).isEqualTo(userModelBla.getName());
			assertThat(u.getPasswordHash()).isNotEmpty();
			// Since we requested an admin user it has the ADMIN role
			assertThat(u.getRoleList()).containsOnly(Role.ADMIN, Role.USER);
			assertThat(u.getStudyList()).isEmpty();
			assertThat(u.getWorker()).isNotNull();
		});

		// Now check with a second user but without admin rights
		// Additionally check that the 'passwordRepeat' is not checked here
		NewUserModel userModelFoo = new NewUserModel();
		userModelFoo.setEmail("foo@foo.org");
		userModelFoo.setName("Foo Foo");
		userModelFoo.setAdminPassword("nobodyCaresAtThisPoint");
		userModelFoo.setPassword("fooPw");
		userModelFoo.setPasswordRepeat("differentPassword");
		userModelFoo.setAdminRole(false);
		jpaApi.withTransaction(() -> {
			userService.bindToUserAndPersist(userModelFoo);
		});
		jpaApi.withTransaction(() -> {
			User u = userDao.findByEmail(userModelFoo.getEmail());
			// Since we didn't requested an admin user it only has the USER role
			assertThat(u.getRoleList()).containsOnly(Role.USER);
		});

		// Clean-up
		testHelper.removeUser(userModelBla.getEmail());
		testHelper.removeUser(userModelFoo.getEmail());
	}

	/**
	 * Test UserService.createAndPersistUser()
	 */
	@Test
	public void checkCreateAndPersistUser() {
		User userBla = new User();
		userBla.setEmail("bla1@bla.org");
		userBla.setName("Bla Bla");

		jpaApi.withTransaction(() -> {
			userService.createAndPersistUser(userBla, "blaPassword", true);
		});

		// Check that the user is stored in the DB properly
		jpaApi.withTransaction(() -> {
			User u = userDao.findByEmail(userBla.getEmail());
			assertThat(u.getEmail()).isEqualTo(userBla.getEmail());
			assertThat(u.getName()).isEqualTo(userBla.getName());
			assertThat(u.getPasswordHash()).isNotEmpty();
			// Since we requested an admin user it has the ADMIN role
			assertThat(u.getRoleList()).containsOnly(Role.ADMIN, Role.USER);
			assertThat(u.getWorker()).isNotNull();
		});

		// Store another user that is not an admin
		User userFoo = new User();
		userFoo.setEmail("foo1@foo.org");
		userFoo.setName("Foo Foo");
		jpaApi.withTransaction(() -> {
			userService.createAndPersistUser(userFoo, "fooPassword", false);
		});
		jpaApi.withTransaction(() -> {
			User u = userDao.findByEmail(userFoo.getEmail());
			// It only has the USER role
			assertThat(u.getRoleList()).containsOnly(Role.USER);
		});

		// Clean-up
		testHelper.removeUser(userBla.getEmail());
		testHelper.removeUser(userFoo.getEmail());
	}

	/**
	 * Test UserService.updatePassword()
	 */
	@Test
	public void checkUpdatePassword() {
		jpaApi.withTransaction(() -> {
			try {
				userService.updatePassword(UserService.ADMIN_EMAIL,
						"newPassword");
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
		});

		jpaApi.withTransaction(() -> {
			userService.authenticate(UserService.ADMIN_EMAIL, "newPassword");
		});
	}

	/**
	 * Test UserService.updatePassword()
	 */
	@Test
	public void checkUpdatePasswordNotFound() {
		jpaApi.withTransaction(() -> {
			try {
				userService.updatePassword("not-existing@user.org",
						"newPassword");
				Fail.fail();
			} catch (NotFoundException e) {
				// A NotFoundException must be thrown
			}
		});
	}

	/**
	 * Test UserService.changeAdminRole(): add or remove the ADMIN role to a
	 * user
	 */
	@Test
	public void checkChangeAdminRole() {
		User userBla = testHelper.createAndPersistUser("bla@bla.org", "Bla Bla",
				"bla");
		defineLoggedInUser(testHelper.getAdmin());

		// Add ADMIN role to user
		jpaApi.withTransaction(() -> {
			try {
				userService.changeAdminRole(userBla.getEmail(), true);
			} catch (NotFoundException | ForbiddenException e) {
				Fail.fail();
			}
		});
		jpaApi.withTransaction(() -> {
			User u = userDao.findByEmail(userBla.getEmail());
			// User has role ADMIN now
			assertThat(u.getRoleList()).containsOnly(Role.USER, Role.ADMIN);
		});

		// Remove ADMIN role from user
		jpaApi.withTransaction(() -> {
			try {
				userService.changeAdminRole(userBla.getEmail(), false);
			} catch (NotFoundException | ForbiddenException e) {
				Fail.fail();
			}
		});
		jpaApi.withTransaction(() -> {
			User u = userDao.findByEmail(userBla.getEmail());
			// User does not have role ADMIN now
			assertThat(u.getRoleList()).containsOnly(Role.USER);
		});

		testHelper.removeUser(userBla.getEmail());
	}

	/**
	 * Test UserService.changeAdminRole(): user must exist
	 */
	@Test
	public void checkChangeAdminRoleUserNotFound() {
		defineLoggedInUser(testHelper.getAdmin());

		jpaApi.withTransaction(() -> {
			try {
				userService.changeAdminRole("non-existing@user.org", false);
				Fail.fail();
			} catch (NotFoundException e) {
				// A NotFoundException must be thrown
			} catch (ForbiddenException e) {
				Fail.fail();
			}
		});
	}

	/**
	 * Test UserService.changeAdminRole(): the user 'admin' can't loose its
	 * ADMIN role
	 */
	@Test
	public void checkChangeAdminRoleAdminAlwaysAdmin() {
		// Put a different user than 'admin' in RequestScope as logged-in
		User userBla = testHelper.createAndPersistUser("bla@bla.org", "Bla Bla",
				"bla");
		defineLoggedInUser(userBla);

		jpaApi.withTransaction(() -> {
			try {
				userService.changeAdminRole(UserService.ADMIN_EMAIL, false);
				Fail.fail();
			} catch (NotFoundException e) {
				Fail.fail();
			} catch (ForbiddenException e) {
				// A ForbiddenException must be thrown
			}
		});

		testHelper.removeUser(userBla.getEmail());
	}

	/**
	 * Test UserService.changeAdminRole():the logged-in user can't loose its
	 * ADMIN rights
	 */
	@Test
	public void checkChangeAdminRoleLoggedInCantLoose() {
		User userBla = testHelper.createAndPersistUser("bla@bla.org", "Bla Bla",
				"bla");
		defineLoggedInUser(testHelper.getAdmin());

		// First add ADMIN role to user
		jpaApi.withTransaction(() -> {
			try {
				userService.changeAdminRole(userBla.getEmail(), true);
			} catch (NotFoundException | ForbiddenException e) {
				Fail.fail();
			}
		});

		// Now make userBla the logged-in user
		defineLoggedInUser(userBla);

		// Try to remove ADMIN role from user
		jpaApi.withTransaction(() -> {
			try {
				userService.changeAdminRole(userBla.getEmail(), false);
				Fail.fail();
			} catch (NotFoundException e) {
				Fail.fail();
			} catch (ForbiddenException e) {
				// A ForbiddenException must be thrown
			}
		});

		// Clean-up
		testHelper.removeUser(userBla.getEmail());
	}

	/**
	 * Test UserService.removeUser()
	 */
	@Test
	public void checkRemoveUser() {
		User userBla = testHelper.createAndPersistUser("bla@bla.org", "Bla Bla",
				"bla");

		// Create a study where the user is member
		Study study = testHelper.createAndPersistExampleStudy(injector,
				userBla.getEmail());

		// Remove user
		jpaApi.withTransaction(() -> {
			try {
				userService.removeUser(userBla.getEmail());
			} catch (NotFoundException | ForbiddenException | IOException e) {
				Fail.fail();
			}
		});

		jpaApi.withTransaction(() -> {
			// User is removed from database
			assertThat(userDao.findByEmail(userBla.getEmail())).isNull();
			// User's studies are removed
			userBla.getStudyList().forEach(s -> {
				assertThat(studyDao.findById(s.getId())).isNull();
			});
			// Study's batches are removed
			study.getBatchList().forEach(s -> {
				assertThat(batchDao.findById(study.getId())).isNull();
			});
		});
	}

	/**
	 * Test UserService.removeUser(): it's not allowed to remove the user
	 * 'admin'
	 */
	@Test
	public void checkRemoveUserNotAdmin() {
		jpaApi.withTransaction(() -> {
			try {
				userService.removeUser(UserService.ADMIN_EMAIL);
			} catch (NotFoundException | IOException e) {
				Fail.fail();
			} catch (ForbiddenException e) {
				// Must throw a ForbiddenException
			}
		});
	}

	private void defineLoggedInUser(User user) {
		testHelper.mockContext();
		RequestScope.put(Authentication.LOGGED_IN_USER, user);
	}

}
