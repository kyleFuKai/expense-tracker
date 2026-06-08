package com.xingzhewk.config;

import com.xingzhewk.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 头像目录。与 UserServiceImpl.avatarDir 同一路径，由 app.upload.avatar-dir 控制。
     * 通过 ResourceHandlerRegistry 映射到 /uploads/avatars/** 暴露为静态资源。
     */
    @Value("${app.upload.avatar-dir:../finance/uploads/avatars}")
    private String avatarDir;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:5500");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Authorization");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/bills/**", "/api/user/**", "/api/categories/**", "/api/budgets/**", "/finance/tags/**")
                .excludePathPatterns("/api/health", "/uploads/**");
    }

    /**
     * 把 /uploads/avatars/** 映射到 avatarDir 目录。
     * Spring 要求 location 以 file:/// 开头才能定位到文件系统路径。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(avatarDir).toAbsolutePath().normalize().toString();
        // Windows 反斜杠转正斜杠，路径末尾必须带 /
        String location = "file:///" + absolute.replace('\\', '/') + "/";
        registry.addResourceHandler("/uploads/avatars/**").addResourceLocations(location);
    }
}
