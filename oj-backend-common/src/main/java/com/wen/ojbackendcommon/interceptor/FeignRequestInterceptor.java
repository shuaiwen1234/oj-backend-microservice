package com.wen.ojbackendcommon.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 1. 获取当前主线程（前端发来的请求）的 Request 对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // 2. 从原始请求中拿到 Cookie
            String cookie = request.getHeader("Cookie");
            if (cookie != null) {
                // 3. 塞给 OpenFeign 远程调用的请求头中
                template.header("Cookie", cookie);
            }
        }
    }
}
