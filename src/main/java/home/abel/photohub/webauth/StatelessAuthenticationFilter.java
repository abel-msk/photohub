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


		//
		//   Option request pas w/o auth for future processing
		//
		if (request.getMethod().equals("OPTIONS")) {
			chain.doFilter(req, res);
			return;
		}


		//
		//    Pretty print for request parameters
		//
		String paramStr = new String("");
		if ( request.getQueryString() != null) {
			String[] parameters = request.getQueryString().split("&");
			for (String parameter : parameters) {
				if ((!parameter.startsWith("callback=")) &&
						(!parameter.startsWith("_="))
						) {
					paramStr += parameter + ",";
				}
			}
			//  Remove last coma
			if ( paramStr.length() > 1 ) {
				paramStr = paramStr.substring(0, paramStr.length() - 1);
			}
			else {
				paramStr = "";
			}
		}
		logger.info(">>> Authentication "+request.getMethod()+" for "+request.getRequestURI()+" options=["+paramStr+"]");

		//headerBuilderService.printReqHeaders(request);
		//headerBuilderService.printDefReqHeaders(request);


		AntPathRequestMatcher matcher = new AntPathRequestMatcher("/api/login/**");
		if ( ! matcher.matches(request) ) {

			Authentication auth = tokenAuthenticationService.getAuthentication((HttpServletRequest) req);
			SecurityContextHolder.getContext().setAuthentication(auth);

			//    Trace all incoming headers
//			if ( logger.isTraceEnabled()) {
//				headerBuilderService.printReqHeaders(request);
//				headerBuilderService.printRespHeaders(response);
//			}

			if (auth == null) {
				logger.debug("Authentication FAIL ");
				//   Add  default, project specific headers to response
				headerBuilderService.buildResponse(request, response);
			} else {
				logger.debug("Authentication SUCCESS for " + auth.getDetails().toString());
			}
		}
		chain.doFilter(req, res); // always continue

		logger.info("<<< Response: " + ((HttpServletResponse) res).getStatus());
		logger.info("<<< Authentication return  method="+request.getMethod()+", path="+request.getRequestURI()+ ", http retcode="+((HttpServletResponse) res).getStatus());
	}
}