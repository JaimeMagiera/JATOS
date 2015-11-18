package services.gui;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import models.common.User;
import play.data.validation.ValidationError;
import utils.common.HashUtils;
import controllers.gui.Authentication;
import daos.common.UserDao;
import exceptions.gui.ForbiddenException;
import exceptions.gui.NotFoundException;
import general.common.MessagesStrings;
import general.gui.RequestScope;

/**
 * Service class mostly for Users controller. Handles everything around User.
 * 
 * @author Kristian Lange
 */
@Singleton
public class UserService {

	public static final String ADMIN_EMAIL = "admin";
	public static final String ADMIN_PASSWORD = "admin";
	public static final String ADMIN_NAME = "Admin";

	private final UserDao userDao;

	@Inject
	UserService(UserDao userDao) {
		this.userDao = userDao;
	}

	/**
	 * Retrieves the user with the given email form the DB. Throws an Exception
	 * if it doesn't exist.
	 */
	public User retrieveUser(String email) throws NotFoundException {
		User user = userDao.findByEmail(email);
		if (user == null) {
			throw new NotFoundException(MessagesStrings.userNotExist(email));
		}
		return user;
	}

	/**
	 * Retrieves the user with the given email form the RequestScope. It was put
	 * into the RequestScope by the AuthenticationAction.
	 */
	public User retrieveLoggedInUser() {
		return (User) RequestScope.get(Authentication.LOGGED_IN_USER);
	}

	/**
	 * Throws an Exception in case the user isn't equal to the loggedInUser.
	 */
	public void checkUserLoggedIn(User user, User loggedInUser)
			throws ForbiddenException {
		if (!user.equals(loggedInUser)) {
			throw new ForbiddenException(
					MessagesStrings.userMustBeLoggedInToSeeProfile(user));
		}
	}

	public User createAdmin() {
		String passwordHash = HashUtils.getHashMDFive(ADMIN_PASSWORD);
		User adminUser = new User(ADMIN_EMAIL, ADMIN_NAME, passwordHash);
		userDao.create(adminUser);
		return adminUser;
	}

	public List<ValidationError> validateNewUser(User newUser, String password,
			String passwordRepeat) {
		List<ValidationError> errorList = new ArrayList<>();

		// Check if user with this email already exists.
		if (userDao.findByEmail(newUser.getEmail()) != null) {
			errorList.add(new ValidationError(User.EMAIL,
					MessagesStrings.THIS_EMAIL_IS_ALREADY_REGISTERED));
		}

		checkPasswords(password, passwordRepeat, errorList);
		return errorList;
	}

	public List<ValidationError> validateChangePassword(User user,
			String password, String passwordRepeat, String oldPasswordHash) {
		List<ValidationError> errorList = new ArrayList<>();

		if (!userDao.authenticate(user.getEmail(), oldPasswordHash)) {
			errorList.add(new ValidationError(User.OLD_PASSWORD,
					MessagesStrings.WRONG_OLD_PASSWORD));
		}

		checkPasswords(password, passwordRepeat, errorList);
		return errorList;
	}

	public void checkPasswords(String password, String passwordRepeat,
			List<ValidationError> errorList) {

		// Check for non empty passwords
		if (password.trim().isEmpty() || passwordRepeat.trim().isEmpty()) {
			errorList.add(new ValidationError(User.PASSWORD,
					MessagesStrings.PASSWORDS_SHOULDNT_BE_EMPTY_STRINGS));
		}

		// Check that both passwords are the same
		String passwordHash = HashUtils.getHashMDFive(password);
		String passwordHashRepeat = HashUtils.getHashMDFive(passwordRepeat);
		if (!passwordHash.equals(passwordHashRepeat)) {
			errorList.add(new ValidationError(User.PASSWORD,
					MessagesStrings.PASSWORDS_DONT_MATCH));
		}
	}

	/**
	 * Creates a user, sets password hash and persists it.
	 */
	public void createUser(User newUser, String password) {
		String passwordHash = HashUtils.getHashMDFive(password);
		newUser.setPasswordHash(passwordHash);
		userDao.create(newUser);
	}

	/**
	 * Change password hash and persist user.
	 */
	public void changePasswordHash(User user, String newPasswordHash) {
		user.setPasswordHash(newPasswordHash);
		userDao.update(user);
	}

	/**
	 * Changes name and persists user.
	 */
	public void updateName(User user, String name) {
		user.setName(name);
		userDao.update(user);
	}

}
