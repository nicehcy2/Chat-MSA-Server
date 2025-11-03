package com.nicehcy2.gatewayserver.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class ResponseFilter {

    final Logger logger =LoggerFactory.getLogger(ResponseFilter.class);

    @Autowired
    Tracer tracer;

    @Autowired
    FilterUtils filterUtils;

    @Bean
    public GlobalFilter postGlobalFilter() {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                Span span = tracer.currentSpan();
                String traceId = span.context().traceId();
                logger.debug("Adding the correlation id to the outbound headers. {}", traceId);
                exchange.getResponse().getHeaders().add(FilterUtils.CORRELATION_ID, traceId);
                logger.debug("Completing outgoing request for {}.", exchange.getRequest().getURI());
            }));
        };
    }
}