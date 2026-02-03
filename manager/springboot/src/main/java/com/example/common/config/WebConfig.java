// java
package com.example.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

// 在 WebConfig.java 中配置拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    // 在 WebConfig.java 中配置拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns("/login", "/register", "/api/tcp/**", "/ws/**", "/map/import");  // 添加排除路径
    }
}

