package com.back_hexiang_studio.securuty;


import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.entity.User;
import com.back_hexiang_studio.GlobalException.ErrorCode;
import com.back_hexiang_studio.GlobalException.UnauthorizedException;
import com.back_hexiang_studio.GlobalException.ForbiddenException;
import com.back_hexiang_studio.GlobalException.GlobalExceptionHandler;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.UserService;
import com.back_hexiang_studio.service.impl.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT过滤器，从请求头提取Token，校验并设置Security上下文
 * 
 * 路径权限控制完全由Spring Security负责
 * 异常处理统一由全局异常处理器负责，确保响应格式一致性
 * 
 * @author Hexiang  
 * @date 2024/09/27
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserService userservice;
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;
    
    // JSON序列化工具，用于异常响应
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 用户状态常量
    private static final String STATUS_ENABLED = "1";
    private static final String STATUS_DISABLED = "0";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("JwtAuthFilter处理请求: {} {}", request.getMethod(), requestURI);

        // 检查是否是白名单路径（不需要认证的接口）
        if (isWhiteListPath(requestURI)) {
            log.debug("JwtAuthFilter: 白名单路径，跳过认证: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // JWT认证逻辑
            authenticateRequest(request, response);
            
            // 认证通过，继续处理请求
            filterChain.doFilter(request, response);
            
        } catch (UnauthorizedException e) {
            // 处理未授权异常
            handleAuthenticationException(response, e);
        } catch (ForbiddenException e) {
            // 处理禁止访问异常
            handleAuthenticationException(response, e);
        } catch (Exception e) {
            // 处理其他未知异常
            log.error("JwtAuthFilter发生未知异常", e);
            handleAuthenticationException(response, new UnauthorizedException(ErrorCode.SYSTEM_ERROR, "系统异常"));
        }
    }

    /**
     * 检查是否是白名单路径（不需要认证的接口）
     * 
     * @param requestURI 请求URI
     * @return 是否是白名单路径
     */
    private boolean isWhiteListPath(String requestURI) {
        // 定义白名单路径
        String[] whiteListPaths = {
            "/admin/user/login",           // 管理员登录接口
            "/admin/captcha/",             // 验证码接口（登录前需要获取）
            "/wx/user/login",              // 微信用户登录接口
            "/wx/user/logout",             // 微信用户登出接口
            "/admin/file/view/",           // 文件查看接口
            "/api/admin/file/view/",       // API前缀文件查看接口
            "/upload/",                    // 静态文件访问接口
            "/wx/file/",                   // 微信端文件操作接口
            "/user/",                      // 用户相关接口
            "/error",                      // Spring Boot 错误页
            "/ai-assistant/stream/",       // 流式接口
            "/api/ai-assistant/stream/",   // API前缀流式接口
            "/api/ai/rag/",               // RAG管理接口
            "/api/public/"                // 公开测试API
        };

        // 检查路径是否匹配白名单
        for (String whiteListPath : whiteListPaths) {
            if (requestURI.startsWith(whiteListPath)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 执行JWT认证逻辑
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @throws UnauthorizedException 未授权异常
     * @throws ForbiddenException 禁止访问异常
     */
    private void authenticateRequest(HttpServletRequest request, HttpServletResponse response) {
        // 从请求头或URL参数获取Token
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer")) {
            // 从Authorization头获取token
            token = authorizationHeader.substring(7);
            log.debug("JwtAuthFilter: 从Authorization头获取token");
        } else {
            // 尝试从URL参数获取token
            String urlToken = request.getParameter("token");
            if (urlToken != null && !urlToken.trim().isEmpty()) {
                token = urlToken;
                log.debug("JwtAuthFilter: 从URL参数获取token");
            }
        }

        // 如果没有找到有效token，抛出未授权异常
        if (token == null || token.trim().isEmpty()) {
            log.warn("JwtAuthFilter: 缺少有效的Authorization头或token参数，URI: {}", request.getRequestURI());
            throw new UnauthorizedException(ErrorCode.TOKEN_MISSING, "未登录或会话已过期");
        }
        log.debug("JwtAuthFilter: 已提取token，长度: {}", token.length());

        // 校验Access Token有效性（双Token模式）
        if (!tokenService.validateAccessToken(token)) {
            log.warn("JwtAuthFilter: Access Token无效，URI: {}", request.getRequestURI());
            throw new UnauthorizedException(ErrorCode.TOKEN_INVALID, "登录已过期，请重新登录");
        }

        // 从token中解析用户ID
        Long userId = jwtUtils.getUserIdFromToken(token);
        log.debug("JwtAuthFilter: 用户ID: {}", userId);

        // 【新增】无感刷新逻辑：检查Token是否即将过期（剩余时间少于5分钟）
        if (jwtUtils.isTokenAboutToExpired(token)) {
            try {
                log.debug("JwtAuthFilter: Access Token即将过期，尝试自动刷新，用户ID: {}", userId);
                String refreshToken = tokenService.getRefreshTokenFromCookie(request);
                if (refreshToken != null) {
                    String newAccessToken = tokenService.refreshAccessToken(refreshToken);
                    if (newAccessToken != null) {
                        // 在响应头中返回新的Access Token，前端会自动更新
                        response.setHeader("X-New-Access-Token", newAccessToken);
                        log.info("JwtAuthFilter: Access Token自动刷新成功，用户ID: {}", userId);
                    } else {
                        log.warn("JwtAuthFilter: Access Token自动刷新失败，Refresh Token无效，用户ID: {}", userId);
                    }
                } else {
                    log.warn("JwtAuthFilter: Access Token自动刷新失败，未找到Refresh Token，用户ID: {}", userId);
                }
            } catch (Exception e) {
                log.warn("JwtAuthFilter: Token自动刷新异常，用户ID: {}，异常: {}", userId, e.getMessage());
                // 刷新失败不影响当前请求，因为当前Access Token还有效（剩余<5分钟）
            }
        }

        // 设置当前用户ID到ThreadLocal中，用于公共字段填充
        UserContextHolder.setCurrentId(userId);
        log.debug("JwtAuthFilter: 已设置当前用户ID到ThreadLocal: {}", userId);

        // 根据用户ID加载用户信息
        User user = userservice.getUserById(userId);

        // 如果用户不存在，清理上下文并抛出未授权异常
        if (user == null) {
            log.error("JwtAuthFilter: 用户不存在，用户ID: {}", userId);
            UserContextHolder.clear();
            throw new UnauthorizedException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        
        // 检查用户状态，如果被禁用，清理上下文并抛出禁止访问异常
        if (STATUS_DISABLED.equals(user.getStatus())) {
            log.warn("JwtAuthFilter: 用户已被禁用, 用户ID: {}", userId);
            UserContextHolder.clear();
            throw new ForbiddenException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }

        // 获取用户权限列表（基于职位ID）
        List<String> permissions = userservice.getPermissionsByRole(user.getPositionId());
        log.debug("JwtAuthFilter: 用户权限: {}", permissions);

        // 创建UserDetailsImpl对象
        UserDetailsImpl userDetails = new UserDetailsImpl(user, permissions);
        
        // 调试日志：打印即将注入到SecurityContext的authorities
        try {
            log.info("Security: 将为用户 {} 设置权限: {}", userId, userDetails.getAuthorities());
        } catch (Exception ignore) {}

        // 构造认证对象
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, new ArrayList<>(userDetails.getAuthorities()));

        // 设置认证详情
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 放在上下文中
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.debug("JwtAuthFilter: 已设置认证信息，当前authorities={}", authenticationToken.getAuthorities());
    }
    
    /**
     * 处理认证异常，调用全局异常处理器并返回统一格式响应
     * 
     * @param response HTTP响应
     * @param exception 认证异常
     * @throws IOException IO异常
     */
    private void handleAuthenticationException(HttpServletResponse response, RuntimeException exception) throws IOException {
        // 清理用户上下文
        UserContextHolder.clear();
        
        // 调用全局异常处理器获取统一格式的错误响应
        Result<?> errorResult;
        if (exception instanceof UnauthorizedException) {
            errorResult = globalExceptionHandler.handleUnauthorizedException((UnauthorizedException) exception);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else if (exception instanceof ForbiddenException) {
            errorResult = globalExceptionHandler.handleForbiddenException((ForbiddenException) exception);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            errorResult = globalExceptionHandler.handleException(exception);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        
        // 设置响应格式
        response.setContentType("application/json;charset=UTF-8");
        
        // 序列化并返回错误响应
        String jsonResponse = objectMapper.writeValueAsString(errorResult);
        response.getWriter().write(jsonResponse);
        
        log.debug("JwtAuthFilter: 已处理认证异常并返回统一格式响应");
    }
}
