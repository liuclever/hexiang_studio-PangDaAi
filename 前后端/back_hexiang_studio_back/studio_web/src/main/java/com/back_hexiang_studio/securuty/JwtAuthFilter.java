package com.back_hexiang_studio.securuty;


import com.back_hexiang_studio.context.UserContextHolder;
import com.back_hexiang_studio.entity.User;
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
import java.util.Arrays;
import java.util.List;

/**
 * JWT过滤器，从请求头提取Token，校验并设置Security上下文
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

    // 定义不需要认证的路径
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/admin/user/login", 
        "/wxUser/login",     // 旧版微信用户登录接口（兼容）
        "/wx/user/login",    // 新版微信用户登录接口
        "/wx/user/logout",   // 微信用户登出接口
        "/admin/file/view/", // 文件查看路径，用于访问头像等静态资源
        "/api/admin/file/view/", // API前缀的文件查看路径
        "/wx/file/view/",    // 微信端文件查看路径
        "/api/wx/file/view/", // API前缀的微信端文件查看路径
        "/upload/",          // 静态文件访问路径
        "/api/public",
        "/api/ai/rag/",      // RAG管理接口，允许匿名访问
        "/error"
    );
    
    // 用户状态常量
    private static final String STATUS_ENABLED = "1";
    private static final String STATUS_DISABLED = "0";
    
    // JSON响应工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 🔧 优化：减少不必要的日志输出
        String requestURI = request.getRequestURI();
        log.debug("JwtAuthFilter处理请求: {} {}", request.getMethod(), requestURI);

        // 对于公开路径，直接放行
        if (isPublicPath(requestURI)) {
            // 🔧 删除冗余日志：公开路径访问非常频繁（如图片等静态资源），不需要记录
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头或URL参数获取Token
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // 从Authorization头获取token
            token = authorizationHeader.substring(7);
            log.debug("JwtAuthFilter: 从Authorization头获取token");
        } else {
            // 尝试从URL参数获取token（用于EventSource等不支持自定义头的请求）
            String urlToken = request.getParameter("token");
            if (urlToken != null && !urlToken.trim().isEmpty()) {
                token = urlToken;
                log.debug("JwtAuthFilter: 从URL参数获取token");
            }
        }

        // 如果没有找到有效token，返回401未授权
        if (token == null || token.trim().isEmpty()) {
            log.warn("JwtAuthFilter: 缺少有效的Authorization头或token参数，URI: {}", requestURI);
            sendUnauthorizedResponse(response, "未登录或会话已过期");
            return;
        }
            // 🔒 安全：不记录完整token，避免敏感信息泄露
            log.debug("JwtAuthFilter: 已提取token，长度: {}", token.length());

            // 校验token有效性
        if (!tokenService.validateToken(token)) {
            log.warn("JwtAuthFilter: token无效，URI: {}", requestURI);
            sendUnauthorizedResponse(response, "登录已过期，请重新登录");
            return;
        }

        // 从token中解析用户ID
                Long userId = jwtUtils.getUserIdFromToken(token);
                log.debug("JwtAuthFilter: 用户ID: {}", userId);

                // 设置当前用户ID到ThreadLocal中，用于公共字段填充
                UserContextHolder.setCurrentId(userId);
                log.debug("JwtAuthFilter: 已设置当前用户ID到ThreadLocal: {}", userId);

                // 根据用户ID加载用户信息
                User user = userservice.getUserById(userId);

        // 如果用户不存在，返回401未授权
        if (user == null) {
            log.error("JwtAuthFilter: 用户不存在，用户ID: {}", userId);
            UserContextHolder.clear();
            sendUnauthorizedResponse(response, "用户不存在");
            return;
        }
        
        // 检查用户状态，如果被禁用，返回403禁止访问
        if (STATUS_DISABLED.equals(user.getStatus())) {
            log.warn("JwtAuthFilter: 用户已被禁用, 用户ID: {}", userId);
            UserContextHolder.clear();
            sendForbiddenResponse(response, "账号已被禁用，请联系管理员");
            return;
        }

                    // 获取用户权限列表（基于职位ID）
                    List<String> permissions = userservice.getPermissionsByRole(user.getPositionId());
                    log.debug("JwtAuthFilter: 用户权限: {}", permissions);

                    // 创建UserDetailsImpl对象
                    UserDetailsImpl userDetails = new UserDetailsImpl(user, permissions);

                    // 构造认证对象
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, new ArrayList<>(userDetails.getAuthorities()));

                    // 设置认证详情
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 放在上下文中
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("JwtAuthFilter: 已设置认证信息");

        // 认证通过，继续处理请求
        filterChain.doFilter(request, response);
    }
    
    /**
     * 判断请求路径是否为公开路径（不需要认证）
     */
    private boolean isPublicPath(String requestURI) {
        return PUBLIC_PATHS.stream().anyMatch(path -> requestURI.startsWith(path));
    }
    
    /**
     * 发送未授权响应（401）
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        // 创建错误响应
        String jsonResponse = objectMapper.writeValueAsString(
            new ErrorResponse(401, message)
        );
        
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * 发送禁止访问响应（403）
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        
        // 创建错误响应
        String jsonResponse = objectMapper.writeValueAsString(
            new ErrorResponse(403, message)
        );
        
        response.getWriter().write(jsonResponse);
            }
    
    /**
     * 错误响应类
     */
    private static class ErrorResponse {
        private final int code;
        private final String msg;
        private final boolean success = false;
        
        public ErrorResponse(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getMsg() {
            return msg;
        }
        
        public boolean isSuccess() {
            return success;
        }
    }
}
