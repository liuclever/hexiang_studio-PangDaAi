package com.back_hexiang_studio.securuty;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security配置类
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级安全注解
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private CustomPermissionEvaluator customPermissionEvaluator;

    /**
     * 密码编译器，使用Bcrypt加密
     * @return
     */
    @Bean
    public PasswordEncoder PasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置方法安全表达式处理器，注入自定义权限评估器
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(customPermissionEvaluator);
        return handler;
    }

    /**
     * 统一的 CORS 配置供 Spring Security 使用
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 使用 allowedOriginPatterns 支持带凭证的匹配（Spring 6）
        config.setAllowedOriginPatterns(java.util.Arrays.asList(
                "http://localhost:5173",
                "https://localhost:5173",
                "http://127.0.0.1:5173",
                "https://127.0.0.1:5173"
        ));
        config.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 配置安全过滤链
     */
        @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // 启用CORS并使用统一配置
                .csrf(csrf -> csrf.disable())  // 禁用CSRF
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)) // 防止XSS攻击
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求权限
                .authorizeHttpRequests(auth -> auth
                        // 公开接口（无需认证）
                        .requestMatchers("/admin/user/login").permitAll() // 管理员登录接口
                        .requestMatchers("/admin/captcha/**").permitAll() // 验证码接口（登录前需要获取）
                        .requestMatchers("/wx/user/login").permitAll()   // 微信用户登录接口
                        .requestMatchers("/wx/user/logout").permitAll()  // 微信用户登出接口
                        .requestMatchers("/admin/file/view/**").permitAll() // 文件查看接口
                        .requestMatchers("/api/admin/file/view/**").permitAll() // API前缀文件查看接口
                        .requestMatchers("/upload/**").permitAll()       // 静态文件访问接口
                     //   .requestMatchers("/wxUser/file/**").permitAll()  // 微信端文件操作接口（旧版兼容）
                        .requestMatchers("/wx/file/**").permitAll()     // 微信端文件操作接口（新版）
                        .requestMatchers("/user/**").permitAll()         // 用户相关接口
                        .requestMatchers("/error").permitAll()            // Spring Boot 错误页放行，避免已提交响应再次鉴权
                        .requestMatchers("/ai-assistant/stream/**").permitAll() // 流式接口放行，避免响应已提交异常
                        .requestMatchers("/api/ai-assistant/stream/**").permitAll() // API前缀流式接口放行
                        .requestMatchers("/api/public/**").permitAll()  // 公开测试API放行
                        // 需要认证的接口
                        .requestMatchers("/ai-assistant/**").authenticated() // AI助手接口需要认证
                        .requestMatchers("/api/ai-assistant/**").authenticated() // API前缀AI助手接口需要认证
                        .requestMatchers("/wx/**").authenticated()        // 微信端接口需要认证
                        .requestMatchers("/api/course/**").authenticated() // API课程接口需要认证
                        .requestMatchers("/api/attendance/**").authenticated() // API考勤接口需要认证
                        .requestMatchers("/api/auth/**").authenticated()  // API认证接口需要认证
                        .requestMatchers("/admin/**").authenticated()     // 管理员接口需要认证，具体权限由@PreAuthorize控制
                        // 其他的接口必须认证
                        .anyRequest().authenticated()
                )
                // 添加jwt过滤器，放在用户名密码认证过滤器前
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}
