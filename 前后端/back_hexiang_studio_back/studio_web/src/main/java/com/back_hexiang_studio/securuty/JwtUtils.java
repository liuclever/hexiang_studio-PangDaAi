package com.back_hexiang_studio.securuty;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import io.jsonwebtoken.Jwts;

@Component
//Component注解，告诉Spring这是一个组件，需要被Spring管理
public class JwtUtils {
    
    // JWT密钥，从配置文件读取
    @Value("${jwt.secret}")
    private String secret;

    // Access Token过期时间（30分钟）
    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    // Refresh Token过期时间（7天）
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;
    
    // 签发人，从配置文件读取
    @Value("${jwt.issuer}")
    private String issuer;

    //根据用户id生成AccessToken
    public String generateAccessToken(Long userId){
        return Jwts.builder()
                .setSubject(userId.toString())//用户id放到subject中
                .claim("type","access")//设置type为access
                .signWith(SignatureAlgorithm.HS512, secret) //使用HS512算法，secret作为密钥
                .setIssuer(issuer)//设置签发人
                .setIssuedAt(new Date())//设置签发时间
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))//设置过期时间
                .compact();//生成token
    }

    //根据用户id生成RefreshToken
    public String generateRefreshToken(Long userId){
        return Jwts.builder()
                .setSubject(userId.toString())//用户id放到subject中
                .claim("type","refresh")//设置type为refresh
                .signWith(SignatureAlgorithm.HS512, secret) //使用HS512算法，secret作为密钥
                .setIssuer(issuer)//设置签发人
                .setIssuedAt(new Date())//设置签发时间
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))//设置7天过期时间
                .compact();//生成token
    }

    //从token中解析 用户id
    public Long getUserIdFromToken(String token){
        //Claims是JWT声明（声明是保存在JWT里的信息）
        Claims claims = Jwts.parser()
                .setSigningKey(secret) //使用密钥解析
                .parseClaimsJws(token) //解析token
                .getBody();//获取token中的信息
        return Long.valueOf(claims.getSubject());//获取用户id
    }

    //  检查access token是否有效
    public boolean isAccessTokenValid(String token){
        try{
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();

            //验证token类型
            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)){
                return false;   //类型不匹配
            }

            // ✅ 修复：未过期返回true
            return !claims.getExpiration().before(new Date());
        }catch (JwtException e){
            //如果解析失败，说明token过期
            return false;
        }
    }

    // 检查refresh token是否有效
    public boolean isRefreshTokenValid(String token){
        try{
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();

            //验证token类型
            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)){
                return false;   //类型不匹配
            }

            // ✅ 修复：未过期返回true
            return !claims.getExpiration().before(new Date());
        }catch (JwtException e){
            //如果解析失败，说明token过期
            return false;
        }
    }

    //获取token剩余时间
    public long getRemainingTime(String token){
      try {
          Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
          Date expiration = claims.getExpiration();
          Long remaining = expiration.getTime() - System.currentTimeMillis();
          return remaining > 0 ? remaining : -1;
      }catch (JwtException e){
          return -1;
      }
    }

    //检查token是否过期 5分钟
    public boolean isTokenAboutToExpired(String token){
        long remainingTime = getRemainingTime(token);
        return remainingTime >0 && remainingTime < 5*60*1000;
    }

    /**
     * 根据用户id生成长期RefreshToken（记住密码用）
     * @param userId 用户ID
     * @return 长期RefreshToken（30天有效期）
     */
    public String generateLongTermRefreshToken(Long userId) {
        long longTermExpiration = 30L * 24 * 60 * 60 * 1000; // 30天

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("type", "refresh")
                .signWith(SignatureAlgorithm.HS512, secret)
                .setIssuer(issuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + longTermExpiration))
                .compact();
    }
}
