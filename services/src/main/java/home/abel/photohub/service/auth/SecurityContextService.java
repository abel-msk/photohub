package home.abel.photohub.service.auth;

public interface SecurityContextService {
	
	/**
	 * In server mode should use spring-security objects for return CurrentAuth context
	 * Get Authentication object (UserServiceDeails)  for the current user
	 * @return
	 * @throws Exception
	 */
	public Object getAuthentication() throws Throwable;

	/**
	 * Check if the current user has role admin and return true, otherwise return false
	 * @return
	 */
	public boolean isAdminContext();
	
}
