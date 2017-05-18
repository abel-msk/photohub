package home.abel.photohub.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import home.abel.photohub.webauth.ResponseHeaders;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class HeaderBuilderService {
	final Logger logger = LoggerFactory.getLogger(HeaderBuilderService.class);
	final String DEFAULT_ORIGIN = "http://localhost:63342";
	/**
	 * 
	 *  Prepare response headers based on request
	 * 
	 * @param request
	 * @return
	 */
	public HttpHeaders getHttpHeader(HttpServletRequest request) {		
		logger.debug("<<< Response OK for "+
				request.getMethod()+" "
				+request.getPathInfo()+" "+
				(request.getQueryString()==null?"":"["+request.getQueryString()+"]")
				);
		
		return getHttpHeaderSilent(request);
	}
	
	public HttpHeaders getHttpHeaderSilent(HttpServletRequest request) {
		HttpHeaders responseHeaders = new HttpHeaders();

		String originStr = DEFAULT_ORIGIN;
		
		if (request != null ) {
			//  Copy headers from request
			Enumeration<String> hdrs =  request.getHeaders("Access-Control-Request-Headers");
			while(hdrs.hasMoreElements()){
				String nextHdrValue = hdrs.nextElement();
				//logger.trace("Copy header Access-Control-Allow-Header: ",nextHdrValue);
				responseHeaders.add("Access-Control-Allow-Headers",(String) nextHdrValue);
			}
			
			//   Set Access-Control-Allow-Origin from referer string
			try {	
				if (request.getHeader("Origin") == null) {
					if (request.getHeader("Referer") != null ) {
						URL refererUrl = new URL(request.getHeader("Referer"));
						originStr = refererUrl.getProtocol()+"://"+refererUrl.getHost() + 
						(refererUrl.getPort() == -1?"":(":"+Integer.toString(refererUrl.getPort())));
					}
				}
				else {
					originStr = request.getHeader("Origin");
				}
				
			} catch (Exception e) {
				logger.warn("Cannot parse referer string. " +  request.getHeader("Referer"));
				originStr=DEFAULT_ORIGIN;
			}
		}
		
		responseHeaders.set("Access-Control-Allow-Origin", originStr);
		
				
		responseHeaders.set("Access-Control-Allow-Credentials","true");
		responseHeaders.set("Access-Control-Allow-Methods", "GET, OPTIONS, POST, DELETE, PUT");
		responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
		responseHeaders.set("Access-Control-Max-Age", "86400");
		//responseHeaders.add("Cache-Control","max-age=0, must-revalidate");

		return responseHeaders;
	}

		
		
		
	/**
	 * 
	 *    Insert headers in to response object
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	public HttpServletResponse buildResponse(HttpServletRequest request, HttpServletResponse response ) {
		
		Set<Map.Entry<String,List<String>>> headersSet = getHttpHeader(request).entrySet();
		Iterator<Entry<String,List<String>>>  itr = headersSet.iterator();
		while (itr.hasNext() ) {
			Entry<String,List<String>> entry =itr.next();
			String headreName = entry.getKey();
			//Set first header value
			if ( entry.getValue() != null) {
				response.setHeader(headreName, entry.getValue().get(0));
			}
			for (int i=1; i < entry.getValue().size(); i++) {
				response.addHeader(headreName, entry.getValue().get(i));
			}
			
		}	
		return response;
	}
	
	
	
	
	/**
	 * Simple HTTP response header set
	 * @return
	 */
	public HttpHeaders getHttpHeader() {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("Access-Control-Allow-Origin", "*");
		responseHeaders.add("Access-Control-Allow-Methods", "GET, OPTIONS, POST, DELETE, PUT, PATCH");
		responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
		responseHeaders.add("Access-Control-Max-Age", "86400");
		//responseHeaders.add("Cache-Control","max-age=0, must-revalidate");
		return responseHeaders;
	}
	
	/**
	 * Print request headers
	 * @param request
	 */
	public void printHeaders(HttpServletRequest request) {
		Set<Map.Entry<String,List<String>>> headersSet = getHttpHeader(request).entrySet();
		Iterator<Entry<String,List<String>>>  itr = headersSet.iterator();
		while (itr.hasNext() ) {
			Entry<String,List<String>> entry =itr.next();
			String headreName = entry.getKey();
			for (int i=1; i < entry.getValue().size(); i++) {
				logger.debug(headreName +" : "+ entry.getValue().get(i));
			}
		}	
	}
	
}
