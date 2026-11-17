package com.safayet.afsos_nama.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.sql.SQLException;
import java.util.UUID;

@Configuration
@Slf4j
public class ErrorDiagnostics implements WebMvcConfigurer, HandlerInterceptor {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/error", "/profile");
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView view) {
        if (view == null || !"error".equals(view.getViewName())) {
            return;
        }
        String reference = UUID.randomUUID().toString().substring(0, 8);
        Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttribute instanceof Integer value ? value : response.getStatus();
        Object failure = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Throwable exception = failure instanceof Throwable value ? value : null;
        String category = category(exception);
        view.addObject("errorReference", reference);
        view.addObject("diagnosticStatus", status);
        view.addObject("diagnosticCode", category);
        // Deliberately omit the request body, query string, cookies, and user details.
        if (exception == null) {
            log.error("Request failed: reference={} status={} category={}", reference, status, category);
        } else {
            log.error("Request failed: reference={} status={} category={}", reference, status, category, exception);
        }
    }

    private String category(Throwable failure) {
        Throwable current = failure;
        String result = "REQUEST_REJECTED";
        for (int depth = 0; current != null && depth < 20; depth++) {
            result = current.getClass().getSimpleName();
            if (current instanceof SQLException sql) {
                String state = sql.getSQLState();
                return "DATABASE_" + (state != null && state.matches("[A-Za-z0-9]{5}") ? state : "UNKNOWN")
                        + "_" + sql.getErrorCode();
            }
            if (current.getCause() == current) break;
            current = current.getCause();
        }
        return result;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if ("/profile".equals(request.getRequestURI()) && "POST".equals(request.getMethod())) {
            if (exception != null) {
                log.error("Profile save request failed", exception);
            } else {
                log.info("Profile save response status={}", response.getStatus());
            }
        }
    }
}
