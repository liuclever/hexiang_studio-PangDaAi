package com.back_hexiang_studio.securuty;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import com.back_hexiang_studio.securuty.*;

@Slf4j
@Component
public class TokenService {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    //有效时间
    private static final long TOKEN_EXPIRE=3600*10000/1000;


    /**
     * 创建Token存在redis中
     * @param userId
     * @param username
     * @return
     */
    public String createToken(Long userId,String username) {
        //生成jwt令牌
        String token=jwtUtils.generateToken(userId);

        //将token存入redis
        String tokenKey = "login:token:" + userId;
        //opsForValue().set(key,value,过期时间,时间单位（秒）)
        redisTemplate.opsForValue().set(tokenKey, token,TOKEN_EXPIRE, TimeUnit.SECONDS);
        log.info("创建token成功 - 用户ID: {}, 用户名: {}, token键: {}, 过期时间: {}秒", userId, username, tokenKey, TOKEN_EXPIRE);

        //存储用户信息
        String userKey = "login:user:" + userId;
        redisTemplate.opsForValue().set(userKey, username,TOKEN_EXPIRE, TimeUnit.SECONDS);
        return token;
    }

    /**
     * 验证令牌是否有效
     * @param token
     * @return
     */
    public Boolean validateToken(String token){

        try{
            //基础jwt验证
            if(!jwtUtils.isTokenValid(token)){
                return false;
            }
            //获取用户id
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                return false;
            }
            //检查redis 中是否 存在对应的token
            String tokenKey = "login:token:" + userId;
            String cachedToken = (String) redisTemplate.opsForValue().get(tokenKey);

            //检查token是否一致
            return token.equals(cachedToken);
        }catch (Exception e){
            return false;
        }
    }
    /**
     * 刷新令牌有效期
     */
    public void refreshToken(Long userId){
        String tokenKey = "login:token:" + userId;
        String userKey= "login:user:" + userId;

        //更新redis中token和用户信息有效期
        redisTemplate.expire(tokenKey, TOKEN_EXPIRE, TimeUnit.SECONDS);
        redisTemplate.expire(userKey, TOKEN_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 注销令牌
     */
    public void logout(Long userId) {
        redisTemplate.delete("login:token:" + userId);
        redisTemplate.delete("login:user:" + userId);
    }

    /**
     * 获取用户信息
     */
    public String getUsernameFromRedis(Long userId) {
        return (String) redisTemplate.opsForValue().get("login:user:" + userId);
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        String tokenKey = "login:token:" + userId;
        boolean isOnline = redisTemplate.hasKey(tokenKey);
        log.debug("检查用户在线状态 - 用户ID: {}, token键: {}, 在线状态: {}", userId, tokenKey, isOnline);
        return isOnline;
    }

}
