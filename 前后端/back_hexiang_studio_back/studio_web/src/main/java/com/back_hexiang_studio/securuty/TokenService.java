package com.back_hexiang_studio.securuty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.TimeUnit;

/**
 * 双Token管理服务
 * 负责Access Token和Refresh Token的生成、验证、刷新和清理
 */
@Slf4j
@Component
public class TokenService {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;  // 30分钟（毫秒）

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration; // 7天（毫秒）



    /**
     * 创建双Token（登录时使用）
     * @param userId 用户ID
     * @param username 用户名
     * @param response HTTP响应（用于设置Cookie）
     * @return Access Token（返回给前端）
     */
    public String createTokenPair(Long userId, String username, HttpServletResponse response) {
        // 1. 生成双Token
        String accessToken = jwtUtils.generateAccessToken(userId);
        String refreshToken = jwtUtils.generateRefreshToken(userId);

        // 2. 存储Access Token到Redis（30分钟）
        String accessKey = "login:access:" + userId;
        redisTemplate.opsForValue().set(accessKey, accessToken,
                accessExpiration, TimeUnit.MILLISECONDS);

        // 3. 存储Refresh Token到Redis（7天）
        String refreshKey = "login:refresh:" + userId;
        redisTemplate.opsForValue().set(refreshKey, refreshToken,
                refreshExpiration, TimeUnit.MILLISECONDS);

        // 4. 存储用户信息（跟随Refresh Token有效期）
        String userKey = "login:user:" + userId;
        redisTemplate.opsForValue().set(userKey, username,
                refreshExpiration, TimeUnit.MILLISECONDS);

        // 5. 设置HttpOnly Cookie存储Refresh Token
        setRefreshTokenCookie(response, refreshToken);

        log.info("双Token创建成功 - 用户ID: {}, 用户名: {}, Access Token过期: {}分钟, Refresh Token过期: {}天",
                userId, username, accessExpiration / (60 * 1000), refreshExpiration / (24 * 60 * 60 * 1000));

        return accessToken;
    }

    /**
     * 验证Access Token是否有效
     * @param accessToken Access Token
     * @return 是否有效
     */
    public boolean validateAccessToken(String accessToken) {
        try {
            // 1. JWT基础验证（签名、过期、类型）
            if (!jwtUtils.isAccessTokenValid(accessToken)) {
                return false;
            }

            // 2. 获取用户ID
            Long userId = jwtUtils.getUserIdFromToken(accessToken);
            if (userId == null) {
                return false;
            }

            // 3. Redis一致性验证
            String accessKey = "login:access:" + userId;
            String cachedToken = (String) redisTemplate.opsForValue().get(accessKey);

            return accessToken.equals(cachedToken);
        } catch (Exception e) {
            log.warn("Access Token验证失败", e);
            return false;
        }
    }

    /**
     * 验证Refresh Token是否有效
     * @param refreshToken Refresh Token
     * @return 是否有效
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            // 1. JWT基础验证（签名、过期、类型）
            if (!jwtUtils.isRefreshTokenValid(refreshToken)) {
                return false;
            }

            // 2. 获取用户ID
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            if (userId == null) {
                return false;
            }

            // 3. Redis一致性验证
            String refreshKey = "login:refresh:" + userId;
            String cachedToken = (String) redisTemplate.opsForValue().get(refreshKey);

            return refreshToken.equals(cachedToken);
        } catch (Exception e) {
            log.warn("Refresh Token验证失败", e);
            return false;
        }
    }

    /**
     * 使用Refresh Token字符串刷新Access Token（用于小程序端）
     * @param refreshToken Refresh Token字符串
     * @return 新的Access Token，null表示刷新失败
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            // 1. 验证Refresh Token
            if (!validateRefreshToken(refreshToken)) {
                log.warn("Refresh Token验证失败，无法刷新");
                return null;
            }

            // 2. 获取用户ID
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);

            // 3. 生成新的Access Token
            String newAccessToken = jwtUtils.generateAccessToken(userId);

            // 4. 更新Redis中的Access Token
            String accessKey = "login:access:" + userId;
            redisTemplate.opsForValue().set(accessKey, newAccessToken,
                    accessExpiration, TimeUnit.MILLISECONDS);

            // 5. 延长Refresh Token和用户信息的有效期（滑动过期）
            String refreshKey = "login:refresh:" + userId;
            String userKey = "login:user:" + userId;
            redisTemplate.expire(refreshKey, refreshExpiration, TimeUnit.MILLISECONDS);
            redisTemplate.expire(userKey, refreshExpiration, TimeUnit.MILLISECONDS);

            log.info("Token刷新成功 - 用户ID: {}, 新Access Token已生成", userId);
            return newAccessToken;

        } catch (Exception e) {
            log.error("Token刷新异常", e);
            return null;
        }
    }

    /**
     * 用户登出（清理所有Token）
     * @param userId 用户ID
     */
    public void logout(Long userId) {
        try {
            // 删除Redis中的所有相关数据
            redisTemplate.delete("login:access:" + userId);
            redisTemplate.delete("login:refresh:" + userId);
            redisTemplate.delete("login:user:" + userId);

            log.info("用户登出完成 - 用户ID: {}, 所有Token已清理", userId);
        } catch (Exception e) {
            log.error("登出清理异常", e);
        }
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        String accessKey = "login:access:" + userId;
        boolean isOnline = redisTemplate.hasKey(accessKey);
        log.debug("检查用户在线状态 - 用户ID: {}, 在线状态: {}", userId, isOnline);
        return isOnline;
    }

    /**
     * 从Redis获取用户名
     * @param userId 用户ID
     * @return 用户名
     */
    public String getUsernameFromRedis(Long userId) {
        return (String) redisTemplate.opsForValue().get("login:user:" + userId);
    }

    /**
     * 根据用户ID获取Refresh Token（用于小程序端）
     * @param userId 用户ID
     * @return Refresh Token
     */
    public String getRefreshTokenByUserId(Long userId) {
        return (String) redisTemplate.opsForValue().get("login:refresh:" + userId);
    }

    /**
     * 从Cookie中提取Refresh Token
     * @param request HTTP请求
     * @return Refresh Token，null表示未找到
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 设置Refresh Token到HttpOnly Cookie
     * @param response HTTP响应
     * @param refreshToken Refresh Token
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);                                 // JavaScript无法访问
        cookie.setMaxAge((int) (refreshExpiration / 1000));       // 7天过期（秒）
        cookie.setPath("/");                                      // 全站有效
        cookie.setSecure(false);                                  // 开发环境HTTP，生产环境需改为true
        // cookie.setSameSite("Lax");                             // Spring Boot 2.6+支持

        response.addCookie(cookie);
        log.debug("Refresh Token Cookie已设置，有效期: {}天", refreshExpiration / (24 * 60 * 60 * 1000));
    }

    /**
     * 清除Refresh Token Cookie
     * @param response HTTP响应
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);                                      // 立即过期
        cookie.setPath("/");
        response.addCookie(cookie);
        log.debug("Refresh Token Cookie已清除");
    }

    /**
     * 创建长期双Token（记住密码时使用）
     * @param userId 用户ID
     * @param username 用户名
     * @param response HTTP响应（用于设置Cookie）
     * @return Access Token（返回给前端）
     */
    public String createLongTermTokenPair(Long userId, String username, HttpServletResponse response) {
        log.info("创建长期Token - 用户ID: {}, 记住密码功能", userId);

        // 1. 生成双Token
        String accessToken = jwtUtils.generateAccessToken(userId);
        String refreshToken = jwtUtils.generateRefreshToken(userId); // 先使用现有方法

        // 2. 存储Access Token到Redis（30分钟）
        String accessKey = "login:access:" + userId;
        redisTemplate.opsForValue().set(accessKey, accessToken,
                accessExpiration, TimeUnit.MILLISECONDS);

        // 3. 存储长期Refresh Token到Redis（30天）
        long longTermExpiration = 30L * 24 * 60 * 60 * 1000; // 30天毫秒数
        String refreshKey = "login:refresh:" + userId;
        redisTemplate.opsForValue().set(refreshKey, refreshToken,
                longTermExpiration, TimeUnit.MILLISECONDS);

        // 4. 存储用户信息（跟随长期Refresh Token有效期）
        String userKey = "login:user:" + userId;
        redisTemplate.opsForValue().set(userKey, username,
                longTermExpiration, TimeUnit.MILLISECONDS);

        // 5. 设置长期HttpOnly Cookie存储Refresh Token（30天）
        setLongTermRefreshTokenCookie(response, refreshToken);

        log.info("长期双Token创建成功 - 用户ID: {}, 用户名: {}, 记住密码: 30天",
                userId, username);

        return accessToken;
    }

    /**
     * 设置长期Refresh Token Cookie（记住密码）
     */
    private void setLongTermRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);              // 防止XSS攻击
        refreshCookie.setSecure(false);               // 开发环境设为false，生产环境设为true
        refreshCookie.setPath("/");                   // Cookie作用域
        refreshCookie.setMaxAge(30 * 24 * 60 * 60);  // 30天有效期（秒）

        response.addCookie(refreshCookie);
        log.debug("长期Refresh Token Cookie已设置 - 有效期: 30天");
    }
}