package home.abel.photohub.service.auth;

import home.abel.photohub.model.User;
import home.abel.photohub.model.UserRepository;
import home.abel.photohub.model.UserRole;
import home.abel.photohub.service.ConfVarEnum;
import home.abel.photohub.service.ConfigService;
import home.abel.photohub.service.ExceptionInternalError;
import home.abel.photohub.service.UserService;

import java.lang.Object;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
/**
 *   Retrieve current bean for single user (standalone) compiled mode.
 *   Return default user for any request.  
 *   User name retrieved from db (See  CONFIG table variable DEFAULT_USER) 
 *   or it can be obtained from property defaultUserName
 *   The default user should have role ADMIN
 *   
 * @author abel
 *
 */
public class SecurityContextSingleUser implements SecurityContextService {
	final Logger logger = LoggerFactory.getLogger(SecurityContextSingleUser.class);

	@Autowired
	ConfigService configService;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	UserService userService;
	
	
	
	
	User currentUserObj = null;
	
	/**
	 *    Return user object for default username.  if theis no deault user in db retun new object for 
	 *    default user with username from property or config.
	 *    otherway generate warning and return User object for username 'default'
	 */
	@Override
	public User getAuthentication() throws Exception {
		User curUserDetails = null;
		
		if ( ! configService.isInstalledAsServer() ) {                   // Check compiled mode
			if (currentUserObj == null) {                                //  Current user info not in cache yet
				String defaultUsername = configService.getValue(ConfVarEnum.DEFAULT_USER);
				if (defaultUsername == null) {
					defaultUsername = "default";
					configService.setValue(ConfVarEnum.DEFAULT_USER,defaultUsername);
				}
				curUserDetails  =  userRepository.findByUsername(defaultUsername);    //  Load user object from DB
				if (curUserDetails == null) { //  Not in db? try to get name from  property and generate new User object for this name
					curUserDetails = createAdminUser(defaultUsername);
				}
				currentUserObj = curUserDetails;                         //  Save loaded user info for future use
			}
			else {  //  Defaul user info alredy loaded 
				curUserDetails = currentUserObj;
			}
		}
		else {
			throw new ExceptionInternalError("Internal error: Do not use 'SecuritySingleUser' bean for non standalone mode.");
		}
		return curUserDetails;
	}
	
	
	/**
	 * Create new 'fake' User Object with username =  'defaultUsername' and add role 'ADMIN'.
	 * Objects placed in memeory and not saved in DB.
	 * @param defaultUsername
	 * @return
	 */
	@Transactional
	private User createAdminUser(String defaultUsername) {
		logger.debug("Create and save Default user object in DB.");
		User curUserDetails = new User();
		curUserDetails.setUsername(defaultUsername);	
		return addUser(defaultUsername,"admin");
	}
	
	private User addUser(String username, String password) {
		User user = new User();
		user.setUsername(username);
		user.setPassword(new BCryptPasswordEncoder().encode(password));
		user.grantRole(username.equals("admin") ? UserRole.ADMIN : UserRole.USER);
		userRepository.save(user);
		return user;
	}	
	
	
	/**
	 * Return username of current user object
	 * @return
	 * @throws Exception
	 */
	public String getCurrentUserName() throws Exception {
		return ((User)getAuthentication()).getUsername();
	}

	/**
	 *    Check current user admin permission.  User has role 'ADMIN'
	 */
	@Override
	public boolean isAdminContext() {
		
		User currentUser = userService.getCurrent();
		Set<UserRole> roles =  currentUser.getRoles();
		for (UserRole item: roles) {
			if ( item.compareTo(UserRole.ADMIN) == 0 ) return true;
		}
		
		return false;
	}

}
