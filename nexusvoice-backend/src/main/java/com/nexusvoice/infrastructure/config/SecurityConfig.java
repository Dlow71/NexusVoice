package com.nexusvoice.infrastructure.config;

import com.nexusvoice.infrastructure.security.JwtAccessDeniedHandler;
import com.nexusvoice.infrastructure.security.JwtAuthenticationEntryPoint;
import com.nexusvoice.infrastructure.security.JwtAuthenticationFilter;
import com.nexusvoice.infrastructure.security.OAuth2LoginSuccessHandler;
import com.nexusvoice.infrastructure.security.OAuth2LoginFailureHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;
    
    @Autowired(required = false)
    private OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    
    @Autowired(required = false)
    private OAuth2LoginFailureHandler oauth2LoginFailureHandler;

    /**
     * WebSocket安全配置
     * 完全忽略WebSocket端点，不经过Spring Security过滤链
     * WebSocket连接的认证由WebSocketJwtInterceptor独立处理
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/ws/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（前后端分离，使用JWT）
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置会话策略：无状态（JWT）
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 配置异步请求支持（SSE需要）
            .securityContext(context -> 
                context.requireExplicitSave(false) // 允许异步请求共享SecurityContext
            )
            
            // 启用CORS配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 公开访问的端点
                .requestMatchers(
                    "/",
                    "/api/health",
                    "/api/auth/**",
                    "/api/admin/auth/login",
                    "/actuator/health",
                    "/actuator/info",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/druid/**",
                    "/error",
                    // OAuth2端点
                    "/oauth2/**",
                    "/login/oauth2/**"
                ).permitAll()
                // 注意：/ws/** 已通过webSecurityCustomizer()完全忽略，不在此配置
                
                // 需要认证的端点
                .anyRequest().authenticated()
            )
            
            // 配置异常处理
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)
            )
            
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        // 配置OAuth2登录（如果启用）
        if (oauth2LoginSuccessHandler != null && oauth2LoginFailureHandler != null) {
            http.oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2LoginSuccessHandler)
                .failureHandler(oauth2LoginFailureHandler)
            );
            log.info("OAuth2登录已启用");
        }
            
        return http.build();
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 允许的源（开发环境允许所有，生产环境应该指定具体域名）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 允许携带认证信息
        configuration.setAllowCredentials(true);
        
        // 预检请求的缓存时间
        configuration.setMaxAge(3600L);
        
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
