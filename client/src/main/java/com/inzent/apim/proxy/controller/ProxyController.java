package com.inzent.apim.proxy.controller;

import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.inzent.apim.proxy.config.HttpConfig;
import com.inzent.apim.proxy.util.ResourceContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ProxyController {
	private final HttpConfig httpConfig;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${proxy.ssl.client.validateCertificate:true}")
	private boolean validateCertificate;

	@RequestMapping("/**")
	public ResponseEntity mirrorRest(	@RequestBody(required = false) final String body,
										final HttpMethod method,
										final HttpServletRequest request,
										final HttpServletResponse response) {

		String queryString = StringUtils.hasText(request.getQueryString())	? "?" + request.getQueryString()
																			: "";

		String xTargetHost = request.getHeader("x-target-host");
		String xReloadHttpClient = request.getHeader("x-reload-httpclient");

		try {
			if (validateCertificate && Boolean.valueOf(xReloadHttpClient)) {
				reloadHttpClient(xTargetHost);
				return ResponseEntity	.status(HttpStatus.CREATED)
										.body("reload httpclient");
			}
			if (Boolean.valueOf(xReloadHttpClient))
				return ResponseEntity	.status(HttpStatus.METHOD_NOT_ALLOWED)
										.body("you have to enable validateCertificate properties ");
			
			if (!StringUtils.hasText(xTargetHost))
				return ResponseEntity	.status(HttpStatus.FORBIDDEN)
										.body("you have to include x-target-host header");

			URI uri = new URI(xTargetHost + request.getRequestURI() + queryString);
			if (log.isDebugEnabled()) {
				URI finalUri = uri;
				log.debug(ResourceContext.withAppendable(builder -> {
					builder	.append("\r\n == proxy target uri ==")
							.append("\nrequest query : ")
							.append(request.getQueryString())
							.append("\nquery : ")
							.append(finalUri.getQuery())
							.append("\nraw query : ")
							.append(finalUri.getRawQuery());
				}));
			}

			HttpHeaders headers = new HttpHeaders();
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();

				String headerValue = request.getHeader(headerName);

				// 헤더 값이 없으면 스킵
				if (!StringUtils.hasText(headerValue)) { continue; }

				if (headerName.equalsIgnoreCase("Transfer-Encoding")) { continue; }
				if (headerName.equalsIgnoreCase("x-target-host")) { continue; }
				// host header는 치환 되기 때문에 삭제
				if (headerName.equalsIgnoreCase("host")) { continue; }

				if (httpConfig.isFilterHeader(headerName)) { continue; }

				headers.set(headerName,
							headerValue);
			}

			// Host header 치환
			headers.set("Host",
						uri.getPort() < 0	? uri.getHost()
											: String.format("%s:%s",
															uri.getHost(),
															uri.getPort()));

			HttpEntity<String> httpEntity = new HttpEntity<>(	body,
																headers);

			ResponseEntity<byte[]> responseEntity = restTemplate.exchange(	uri,
																			method,
																			httpEntity,
																			byte[].class);

			HttpHeaders responseEntityHeaders = responseEntity.getHeaders();
			HttpHeaders newHeaders = new HttpHeaders();
			for (Entry<String, List<String>> h : responseEntityHeaders.entrySet()) {
				if (h	.getKey()
						.equals("Transfer-Encoding"))
					continue;
				for (String v : h.getValue()) { newHeaders.add(	h.getKey(),
																v); }
			}

			return new ResponseEntity<>(responseEntity.getBody(),
										newHeaders,
										responseEntity.getStatusCode());
		}
		catch (HttpStatusCodeException e) {
			return ResponseEntity	.status(e.getRawStatusCode())
									.headers(e.getResponseHeaders())
									.body(e.getResponseBodyAsString());
		}
		catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity	.status(HttpStatus.INTERNAL_SERVER_ERROR)
									.body(e.getMessage());
		}
	}

	private boolean reloadHttpClient(String xTargetHost) throws Exception {
		String hostName = xTargetHost.replaceFirst(	"^(http[s]?://www\\.|http[s]?://|www\\.)",
													"");
		httpConfig.renewCA(hostName);
		restTemplate = httpConfig.loadRestTemplate();
		return false;
	}
}