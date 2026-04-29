package com.gymrat.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class SpaFallbackAdvice {

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResource(NoResourceFoundException ex, HttpServletRequest request) throws NoResourceFoundException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/") || uri.startsWith("/h2-console") || hasFileExtension(uri)) {
            throw ex;
        }
        return "forward:/index.html";
    }

    private boolean hasFileExtension(String uri) {
        int lastSlash = uri.lastIndexOf('/');
        int lastDot = uri.lastIndexOf('.');
        return lastDot > lastSlash;
    }
}
