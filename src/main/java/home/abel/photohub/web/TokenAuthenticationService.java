package home.abel.photohub.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import home.abel.photohub.model.User;
import home.abel.photohub.service.TokenService;
import home.abel.photohub.service.auth.UserAuthentication;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class TokenAuthenticationService {
	final Logger logger = LoggerFactory.getLogger(TokenAuthenticationService.class);

	public static final String AUTH_HEADER_NAME = "x-auth-token";
	private static final long ONE_DAY_MILLIS = 1000 * 60 * 60 * 24;
	private static final int ONE_DAY_SECS = 60 * 60 * 24;
	private static final int EXP_DAYS = 10;

	@Autowired
	TokenService tokenHandler;
	
	@Autowired
	HeaderBuilderService headerBuilderService;
	
//	private final TokenHandler tokenHandler;
//
//	@Autowired
//	public TokenAuthenticationService(@Value("${token.secret}") String secret) {
//		tokenHandler = new TokenHandler(DatatypeConverter.parseBase64Binary(secret));
//	}

	/**
	 *    Create auth token by username and password
	 * 
	 * @param response
	 * @param authentication
	 */
	public void addAuthentication(HttpServletResponse response, UserAuthentication authentication) {
		final User user = authentication.getDetails();
		user.setExpires(System.currentTimeMillis() + (ONE_DAY_MILLIS * EXP_DAYS));
		String tokenString = tokenHandler.createTokenForUser(user);
		logger.debug("Create auth token = " + 
				tokenString.subSequence(0, 5) + 
				"*****" + 
				tokenString.subSequence(tokenString.length()-5, tokenString.length()) 
				);
		
		response.addHeader(AUTH_HEADER_NAME, tokenString);		
		response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
//        try {
//        	tokenString = URLEncoder.encode(tokenString, "UTF-8");
//        } catch (Throwable e) {
//        	logger.error("Url encode error: ",e);
//        }
        Cookie cookie = new Cookie(AUTH_HEADER_NAME, tokenString);
        logger.trace("Send token as coockie " + cookie.getValue());
        cookie.setPath("/");
        cookie.setSecure(false);
        cookie.setMaxAge(ONE_DAY_SECS * EXP_DAYS);
		response.addCookie(cookie);
        
        try {
//			response.getWriter().write("{\"" + AUTH_HEADER_NAME +  "\":\"" + tokenString +"\"," +
//					"\"expires\":" + EXP_DAYS  +
//					"}" );
			response.getWriter().write("{\"" + AUTH_HEADER_NAME +  "\":\"" + tokenString +"\"," +
					"\"expires\":" + EXP_DAYS  +
					"}" );
		} catch (Throwable e) {
			logger.error("Url encode error: ",e);
		}
	}
	
	
	/**
	 * 
	 * Prepare and return authentication token 
	 * 
	 * @param response
	 * @param authentication
	 * @return
	 */
	public String generateToken(HttpServletResponse response, UserAuthentication authentication) {
		final User user = authentication.getDetails();
		user.setExpires(System.currentTimeMillis() + (ONE_DAY_MILLIS * EXP_DAYS));
		String tokenString = tokenHandler.createTokenForUser(user);
		logger.debug("Create auth token = " +  tokenString.subSequence(0, 5) +  "*****" + 
				tokenString.subSequence(tokenString.length()-5, tokenString.length()) 
				);
		
		return tokenString;
	}
	
	/**
	 *   Check for header or cookie for auth token.  if token exist, do authentication.
	 *   
	 * @param request
	 * @return
	 */
	public Authentication getAuthentication(HttpServletRequest request) {
		
		//  Prepare response headers
		//headerBuilderService.getHttpHeader(request);
		
		// Process token
		String token = null;
		token = request.getHeader(AUTH_HEADER_NAME);
		if  (token == null) {
			//logger.debug("Auth header not found. Looking for cookie");
			Cookie[] cookie = request.getCookies();

			if (cookie != null  ) {
				for (int i = 0; i < cookie.length; i++) {
					//logger.trace("Auth Check cookie + " + cookie[i].getName());
					if (cookie[i].getName().equals(AUTH_HEADER_NAME)) {
						try {
							//token = URLDecoder.decode(cookie[i].getValue(), "UTF-8");
							token = cookie[i].getValue();							
						} catch (Exception e) {
							logger.warn("Auth token found but cannot be decoded.",e);
							token = null;
						}
						break;
					}
				}
			}
		}

		
		if (token != null) {
			
//			logger.trace("Token before decoding = " + 
//					token.subSequence(0, 5) +  "*****" +  token.subSequence(token.length()-5, token.length()) 
//					);
			
//			try {
//				token = URLDecoder.decode(token, "UTF-8");
//				//logger.debug("Decoded token =  "+ token); 
//			} catch (UnsupportedEncodingException e) {
//				logger.warn("Auth token found but cannot be decoded.",e);
//				return null;
//			}
			final User user = tokenHandler.parseUserFromToken(token);
			
			if (user != null) {
				logger.debug("Request auth token. User = "+ user.getUsername() +", token = " + 
						token.subSequence(0, 5) + 
						"*****" + 
						token.subSequence(token.length()-5, token.length()) 
						);
				
				return new UserAuthentication(user);
			}
		}
		logger.warn("Request authentication token '"+AUTH_HEADER_NAME+"' not found.");
		return null;
	}
}
