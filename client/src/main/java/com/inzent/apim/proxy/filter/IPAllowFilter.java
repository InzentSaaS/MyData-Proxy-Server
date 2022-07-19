package com.inzent.apim.proxy.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.inzent.apim.proxy.util.Util;

/**
 * @author Na
 */
@Component
public class IPAllowFilter extends OncePerRequestFilter {
	private String denyMessage = "Not allow ip";

	@Value("${proxy.ip.allow:}")
	private String ip;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if(StringUtils.isEmpty(ip)) {
			chain.doFilter(request, response);
			return;
		}
		
		boolean allowedIP = Util.allowIP(request, ip);
		if (!allowedIP) {
			response.getWriter().print(denyMessage);
			((HttpServletResponse)response).setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		chain.doFilter(request, response);
	}
}