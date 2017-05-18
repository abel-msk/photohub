package home.abel.photohub.webauth;

import home.abel.photohub.web.HeaderBuilderService;
import home.abel.photohub.web.TokenAuthenticationService;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

class StatelessAuthenticationFilter extends GenericFilterBean {
	final Logger logger = LoggerFactory.getLogger(StatelessAuthenticationFilter.class);

	private final TokenAuthenticationService tokenAuthenticationService;
	private final  HeaderBuilderService headerBuilderService;

	protected StatelessAuthenticationFilter(TokenAuthenticationService taService, HeaderBuilderService  headerBuilderService) {
		this.tokenAuthenticationService = taService;
		this.headerBuilderService = headerBuilderService;
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
			ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        //logger.trace("Filter requrst " + request.getMethod() + ", to " + request.getPathInfo());
        
		// Skeep auth for any options requests
		if (request.getMethod().equals("OPTIONS")) {
			chain.doFilter(req, res);
    		return;
        }
		
		AntPathRequestMatcher matcher = new AntPathRequestMatcher("/api/login/**");
		if ( matcher.matches(request) ) {
			chain.doFilter(req, res);
    		return;
        }		
		


		Authentication auth = tokenAuthenticationService.getAuthentication((HttpServletRequest) req);
		SecurityContextHolder.getContext().setAuthentication(auth);	
		
		if (auth == null) {
			logger.debug("Authentication fail for request \""+ request.getRequestURI() + "\", method = \"" + request.getMethod()+"\"");			
			//   Add  default, project specific headers to response
			headerBuilderService.buildResponse(request,response);
		}
		else {
			logger.debug("Authentication success for request \""+ request.getRequestURI() + "\", method = \"" + request.getMethod()+"\"");
		}
		
		chain.doFilter(req, res); // always continue
	}
}