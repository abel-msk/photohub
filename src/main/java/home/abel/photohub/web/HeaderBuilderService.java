package home.abel.photohub.web;

import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    private static final String[] DEF_REQ_HEADERS_ARRAY = {"origin","user-agent","referer","host"};

    private static final String[] DEF_RESP_HEADERS_ARRAY = {
            "Access-Control-Allow-Headers",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Access-Control-Allow-Methods",
            "Access-Control-Max-Age"
    };

	/**
	 * 
	 *   Prepare response headers based on request
     *   Создает набори заголовков которые надо добавлять к стандартному  HTTP  ответу
     *   В каждом заголовке вставляет необходимые по умолчанию параметры
     *   Некоторые данные в заголовке ответа могут потребовать значений из заголовков запроса
     *   После завершения формирования заголовка, вормирует сообщение в канале логирования
     *
     *
	 * @param request Запроса для которого необходимо сгенерировать заголовки ответа
	 * @return Набор заголовков которые необходимо добавить к HTTP ответу
	 */
	public HttpHeaders getHttpHeader(HttpServletRequest request) {
		HttpHeaders retHeaders = getHttpHeaderSilent(request);


		logger.debug("<<< Response OK for "+
				request.getMethod()+" "
				+request.getPathInfo());
		return retHeaders;
	}


    /**
     *   Создает набори заголовков которые надо добавлять к стандартному  HTTP  ответу
     *   В каждом заголовке вставляет необходимые по умолчанию параметры
     *   Некоторые данные в заголовке ответа могут потребовать значений из заголовков запроса
     *
     * @param request Запроса для которого необходимо сгенерировать заголовки ответа
     * @return Набор заголовков которые необходимо добавить к HTTP ответу
     */
	public HttpHeaders getHttpHeaderSilent(HttpServletRequest request) {
		HttpHeaders responseHeaders = new HttpHeaders();
		String originStr = DEFAULT_ORIGIN;

        //logger.trace("getHttpHeaderSilent. Generate response headers for '"+request.getRequestURI()+"' with method="+request.getMethod());

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
		
		//responseHeaders.set("Access-Control-Allow-Origin", originStr);
		//responseHeaders.set("Access-Control-Allow-Credentials","true");
		responseHeaders.set("Access-Control-Allow-Methods", "GET, OPTIONS, POST, DELETE, PUT");
		responseHeaders.add("Access-Control-Allow-Headers", "Content-Type,X-Requested-With,Accept,Origin");
		responseHeaders.set("Access-Control-Max-Age", "86400");
		//responseHeaders.add("Cache-Control","max-age=0, must-revalidate");

		return responseHeaders;
	}

	/**
	 * 
	 *    Insert headers in to response object
     *    Процедура генерирует пустой ответ, со вставленными в него заголовками из запроса.
	 * 
	 * @param request  HTTP запрос
	 * @param response HTTP ответ
	 * @return
	 */
	public HttpServletResponse buildResponse(HttpServletRequest request, HttpServletResponse response ) {

	    logger.trace("Build empty response");
		Set<Map.Entry<String,List<String>>> headersSet = getHttpHeaderSilent(request).entrySet();

		for (Entry<String,List<String>> entry : headersSet) {
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
     *    Выводит список всех заголовков в http запросе в канал логирования с приоритетом "trace"
     * @param request HTTP запрос
     */
    public  void printReqHeaders(HttpServletRequest request) {
        printReqHeaders(request,null);
    }
    public  void printDefReqHeaders(HttpServletRequest request) {
        printReqHeaders(request,DEF_REQ_HEADERS_ARRAY);
    }
    /**
     *    Выводит список всех заголовков в http запросе в канал логирования с приоритетом "trace"
     * @param request HTTP запрос
     */
    public  void printReqHeaders(HttpServletRequest request, String[] hdrPrintList) {


        if ( hdrPrintList != null)
            for ( String printheaderName :hdrPrintList) {
                logger.trace("Request header :  "+printheaderName+" - "+request.getHeader(printheaderName));
            }
        else {
            Enumeration<String> hdrNames  = request.getHeaderNames();
            while (hdrNames.hasMoreElements()) {
                String hdrName = hdrNames.nextElement();
                logger.trace("Request header :  "+hdrName+" - "+request.getHeader(hdrName));
//                Enumeration<String> values = request.getHeaders(hdrName);
//                while (values.hasMoreElements()) {
//                    logger.trace("Request header :  "+hdrName+" - "+values.nextElement());
//                }
            }
        }
    }

    /**
     * Выводит список всех заголовков в http ответе в канал логирования с приоритетом "trace"
     * @param response HTTP запрос
     */
    public void printRespHeaders(HttpServletResponse response) {
        for (String headerName : response.getHeaderNames()) {
            for (String value : response.getHeaders(headerName)) {
                logger.trace("Response Header : "+headerName +" - "+ value);
            }
        }
    }
}
