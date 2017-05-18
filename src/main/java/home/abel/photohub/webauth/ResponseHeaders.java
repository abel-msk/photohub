package home.abel.photohub.webauth;

import home.abel.photohub.web.TokenAuthenticationService;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ResponseHeaders {
	final Logger logger = LoggerFactory.getLogger(ResponseHeaders.class);
	
	
	public static ServletResponse setDefaultHeaders(ServletRequest req, ServletResponse res) {
		
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        return (ServletResponse)setDefaultHeaders(request,response);
	}
	
	public static HttpServletResponse setDefaultHeaders(HttpServletRequest request,  HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");	
		response.addHeader("Access-Control-Allow-Methods", "GET, OPTIONS, POST, DELETE, PUT, PATCH");
		//response.addHeader("Access-Control-Allow-Headers", TokenAuthenticationService.AUTH_HEADER_NAME);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        return response;
	}

	public static ServletResponse setAuthHeaders(ServletRequest req, ServletResponse res) {
        //HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
		response.addHeader("Access-Control-Allow-Headers", TokenAuthenticationService.AUTH_HEADER_NAME);
		response.addHeader("Access-Control-Allow-Headers","Origin, X-Requested-With, Content-Type, Accept");
		return (ServletResponse) response;
	}

	
	public static ServletResponse setNoCacheHeaders(ServletRequest req, ServletResponse res) {
        //HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Cache-Control", "no-cache");
		return (ServletResponse) response;
	}	
	
}
