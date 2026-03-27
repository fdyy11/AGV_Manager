// java
package com.example.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

// 在 WebConfig.java 中配置拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    // 配置静态资源处理器
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置文件上传目录的静态资源访问
        String filePath = System.getProperty("user.dir") + "/files/";
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + filePath);
    }

    // 在 WebConfig.java 中配置拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns("/login", "/register", "/api/tcp/**", "/ws/**", "/map/import", "/files/**");  // 添加排除路径
    }
}

