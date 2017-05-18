package home.abel.photohub.webauth;

import home.abel.photohub.model.User;
import home.abel.photohub.service.auth.UserAuthentication;
import home.abel.photohub.web.TokenAuthenticationService;
import home.abel.photohub.webconfig.standalone.AppInit;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

class StatelessLoginFilter extends AbstractAuthenticationProcessingFilter {
	final Logger logger = LoggerFactory.getLogger(StatelessLoginFilter.class);

	private final TokenAuthenticationService tokenAuthenticationService;
	private final UserDetailsService userDetailsService;
	private AntPathRequestMatcher matcher = null;
	
	protected StatelessLoginFilter(String urlMapping,
			TokenAuthenticationService tokenAuthenticationService,
			UserDetailsService userDetailsService,
			AuthenticationManager authManager) {
		
		super( new AntPathRequestMatcher(urlMapping));		
		matcher = new AntPathRequestMatcher(urlMapping);
		this.userDetailsService = userDetailsService;
		this.tokenAuthenticationService = tokenAuthenticationService;
		setAuthenticationManager(authManager);
	}

	/** ------------------------------------------------------------------------
	 *   Check if this filter should be processed with current request
	 ------------------------------------------------------------------------ */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, final FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
    	
        if ( ! matcher.matches(request) ) {
        	chain.doFilter(request, response); 
        	return;
        };
        
		//logger.debug("Filter request "+ request.getRequestURI() + ", method = " + request.getMethod());
		res = ResponseHeaders.setAuthHeaders(req,res);
    	res = ResponseHeaders.setDefaultHeaders(req,res);
		
        if (request.getMethod().equals("POST") ) {
        	super.doFilter(req, res, chain); 
        }
        else if (request.getMethod().equals("OPTIONS")) {
    		response.setStatus(HttpStatus.OK.value());
        }
        else {
        	res = ResponseHeaders.setDefaultHeaders(req,res);
        	response.setStatus(HttpStatus.BAD_REQUEST.value());
        }
        return;
    }	

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {

		//  Check request TYPE
		
		final User user = new ObjectMapper().readValue(request.getInputStream(), User.class);
		final UsernamePasswordAuthenticationToken loginToken = new UsernamePasswordAuthenticationToken(
				user.getUsername(), user.getPassword());
		
		return getAuthenticationManager().authenticate(loginToken);
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain, Authentication authentication) throws IOException, ServletException {

		// Lookup the complete User object from the database and create an Authentication for it
		final User authenticatedUser = (User)userDetailsService.loadUserByUsername(authentication.getName());
		final UserAuthentication userAuthentication = new UserAuthentication(authenticatedUser);

		// Add the custom token as HTTP header to the response
		tokenAuthenticationService.addAuthentication(response, userAuthentication);

		logger.debug("User="+authenticatedUser.getUsername()+ " login successfuly. Response with token.");
		
		// Add the authentication to the Security context
		SecurityContextHolder.getContext().setAuthentication(userAuthentication);
	}
}