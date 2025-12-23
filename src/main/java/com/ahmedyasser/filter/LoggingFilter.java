//package com.ahmedyasser.filter;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//import org.springframework.web.util.ContentCachingRequestWrapper;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.Collections;
//
//@Component
//public class LoggingFilter extends OncePerRequestFilter {
//
//    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
//
//    @Override
//    protected void doFilterInternal(
//            HttpServletRequest request,
//            HttpServletResponse response,
//            FilterChain filterChain)
//            throws ServletException, IOException {
//
//        // Wrap request so body can be read multiple times
//        ContentCachingRequestWrapper wrappedRequest =
//                new ContentCachingRequestWrapper(request);
//
//        log.info(">>> Incoming request");
//        log.info("Method  : {}", wrappedRequest.getMethod());
//        log.info("URI     : {}", wrappedRequest.getRequestURI());
//        log.info("Query   : {}", wrappedRequest.getQueryString());
//
//        Collections.list(wrappedRequest.getHeaderNames()).forEach(header ->
//                log.info("Header  : {} = {}", header, wrappedRequest.getHeader(header))
//        );
//
//        // Continue filter chain FIRST
//        filterChain.doFilter(wrappedRequest, response);
//
//        // Read body AFTER chain (important!)
//        byte[] content = wrappedRequest.getContentAsByteArray();
//        if (content.length > 0) {
//            String body = new String(content, StandardCharsets.UTF_8);
//            log.info("Body    : {}", body);
//        } else {
//            log.info("Body    : <empty>");
//        }
//
//        log.info("<<< End request");
//    }
//}
