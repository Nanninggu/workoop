package com.ptkt.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Vue Router의 HTML5 history 모드를 지원하기 위한 SPA 폴백 필터.
 * /api, /h2-console, 정적 파일(.js, .css, .svg 등)을 제외한 모든 경로를
 * index.html로 포워딩합니다.
 */
@Component
public class SpaFallbackFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        // API, WebSocket, H2 콘솔, 정적 파일은 그대로 통과
        if (path.startsWith("/api/")
                || path.startsWith("/ws")
                || path.startsWith("/h2-console")
                || path.contains(".")) {
            chain.doFilter(request, response);
            return;
        }

        // 나머지 경로(Vue Router 경로)는 index.html로 포워딩
        req.getRequestDispatcher("/index.html").forward(request, response);
    }
}
