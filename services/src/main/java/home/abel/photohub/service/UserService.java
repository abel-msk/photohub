package home.abel.photohub.service;

import javax.annotation.PostConstruct;

import home.abel.photohub.model.QUser;
import home.abel.photohub.model.User;
import home.abel.photohub.model.UserRepository;
import home.abel.photohub.model.UserRole;
import home.abel.photohub.service.auth.ExceptionIncorrectFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import home.abel.photohub.service.auth.UserAuthentication;

@Service
public class UserService {
//implements org.springframework.security.core.userdetails.UserDetailsService {
	final Logger logger = LoggerFactory.getLogger(UserService.class);
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	ConfigService configService;
	
	@Autowired
	private SiteService siteSvc;
	
	private final AccountStatusUserDetailsChecker detailsChecker = new AccountStatusUserDetailsChecker();

	
	public UserService() {
		
	}
	
	/******************************************************************
	 *    Get User
	 */
	//@Override
	public User loadUserByUsername(String username) throws UsernameNotFoundException {
		final User user = userRepository.findByUsername(username);
		if (user == null) {
			throw new UsernameNotFoundException("user not found");
		}
		detailsChecker.check(user);
		return user;
	}
	
	public User getUser(Long userId) throws UsernameNotFoundException {
		 User user = userRepository.findOne(userId);
		if (user == null) {
			throw new UsernameNotFoundException("user not found");
		}
		return user;
	}
	
	public User getUser(String username) throws UsernameNotFoundException {
		User user = userRepository.findOne(QUser.user.username.eq(username));
		if (user == null) {
			throw new UsernameNotFoundException("user not found");
		}
		return user;
	}	
		
	/******************************************************************
	 * Check for Security context defined in spring configuration
	 * Example:
	 * @Bean
	 * SecurityContextService securityContextService() {
	 * 		SecurityContextService scs = new SecurityContextSingleUser();
	 * 		return scs;
	 * }
	 */
	@PostConstruct
	public void Init() {
		//Assert.notNull(context,"SecurityContextService bean was defined !!!");
		logger.debug("USERSERVICE is ready");
	}
	
	/******************************************************************
	 *  Add user
	 */	
	//@Transactional
	public User addUser(String username, String password) {
		User theUser = new User();
		theUser.setUsername(username);
		theUser.setPassword(password);
		theUser.grantRole(UserRole.USER);
		theUser.setExpires(0);
		return addUser(theUser);
	}
	
	/******************************************************************
	 *	Add user by User object 
	 */		
	//@Transactional
	public User addUser(User theUser) {
		if (theUser.getPassword() == null ) {
			theUser.setPassword(theUser.getUsername());
		}
		theUser.setPassword(new BCryptPasswordEncoder().encode(theUser.getPassword()));
		theUser.setEnabled(false);
		userRepository.save(theUser);
		return theUser;
	}
	
	/******************************************************************
	 *  Update user information  and return updates user object
	 */	
	public User updateUser(User theUser) throws Exception{
		User origUser = userRepository.findOne(theUser.getId());
		if ( origUser == null) {
			throw  new UsernameNotFoundException("user not found");
		}
		origUser.setRoles(theUser.getRoles());
		if (theUser.getPassword() != null) {
			origUser.setPassword(new BCryptPasswordEncoder().encode(theUser.getPassword()));
		}
		userRepository.save(origUser);
		return origUser;
	}
	
	/******************************************************************
	 *  Return list of all users 
	 */
	@Transactional
	public Iterable<User> getUsersList() throws Exception {
		//  Only admin can create new user so check if current user is admin
		//Boolean isAdmin = context.isAdminContext();
		//logger.debug("Check curent config.  Context is="+isAdmin);
		//if (! isAdmin ) throw new ExceptionAccessDeny("Insufficient permissions. Only administrator can craete new users.");
		return userRepository.findAll();
	}
	
	/******************************************************************
	 * Delete user and their Authorities
	 * 
	 * @param username
	 */
	public void deleteUser(String username) throws Exception  {
		User currentUserObj = userRepository.findByUsername(username);
		if (currentUserObj == null)  throw  new UsernameNotFoundException("user not found");
		deleteUser(currentUserObj);
	}
	
	public void deleteUser(User user) throws Exception  {
		if (user.getUsername() == configService.getValue(ConfVarEnum.DEFAULT_USER)) {
			throw new ExceptionIncorrectFormat("Try to delete admin user.");
		}
		userRepository.delete(user);
	}	
	
	public void deleteUser(Long userId) throws Exception  {
		User user = userRepository.findOne(userId);
		if (user == null) {
			throw new UsernameNotFoundException("user not found");
		}	
		deleteUser(user);
	}	
		
	/******************************************************************
	 * Return currently authenticated UserDetails
	 * 
	 * @return
	 */
	public User getCurrent() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof UserAuthentication) {
			return ((UserAuthentication) authentication).getDetails();
		}
		return new User(authentication.getName()); //anonymous user support
	}
	
	/******************************************************************
	 * Change User password
	 * 
	 * @param user
	 */
	public void changePassword(String username, String password) throws ExceptionIncorrectFormat {
		User newUser = new User(username);
		newUser.setPassword(password);
		changePassword(newUser);
	}
	
	public void changePassword(User user) throws ExceptionIncorrectFormat {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		final User currentUser = userRepository.findByUsername(authentication.getName());

		if (user.getNewPassword() == null || user.getNewPassword().length() < 4) {
			throw new ExceptionIncorrectFormat("New password to short.");
		}
		final BCryptPasswordEncoder pwEncoder = new BCryptPasswordEncoder();
		if (!pwEncoder.matches(user.getPassword(), currentUser.getPassword())) {
			throw new ExceptionIncorrectFormat("Old password mismatch");
		}
		currentUser.setPassword(pwEncoder.encode(user.getNewPassword()));
		userRepository.save(currentUser);
	}
	
	
	/******************************************************************
	 *    Grant new role to user
	 * 
	 * @param user
	 */	
	public boolean grantRole(User user, UserRole role) throws ExceptionIncorrectFormat {
		if (user == null) {
			throw new ExceptionIncorrectFormat("Invalid user id.");
		}
		user.grantRole(role);
		userRepository.save(user);
		return true;
	}
	
	/******************************************************************
	 *    Remove role from user
	 * @param user
	 * @param role
	 * @return
	 */
	public boolean revokeRole(User user, UserRole role) {
		if (user == null) {
			throw new ExceptionIncorrectFormat("Invalid user id.");
		}
		user.revokeRole(role);
		userRepository.save(user);
		return true;
	}
	
	/******************************************************************
	 *   Update User object in db
	 * @param user
	 */
	public void update(User user) {
		userRepository.save(user);
	}

	
}
