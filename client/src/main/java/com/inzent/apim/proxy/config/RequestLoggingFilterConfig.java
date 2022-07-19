package com.inzent.apim.proxy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingFilterConfig {
	@Value("${proxy.logging.includeQueryString:true}")
	private boolean includeQueryString = true;
	@Value("${proxy.logging.includePayload:true}")
	private boolean includePayload = true;
	@Value("${proxy.logging.maxPayloadLength:10000}")
	private int maxPayloadLength = 10000;
	@Value("${proxy.logging.includeHeaders:true}")
	private boolean includeHeaders = true;
	@Value("${proxy.logging.afterMessagePrefix:REQUEST DATA : }")
	private String AfterMessagePrefix = "REQUEST DATA : ";
	
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter
          = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(includeQueryString);
        filter.setIncludePayload(includePayload);
        filter.setMaxPayloadLength(maxPayloadLength);
        filter.setIncludeHeaders(includeHeaders);
        filter.setAfterMessagePrefix(AfterMessagePrefix);
        return filter;
    }
}