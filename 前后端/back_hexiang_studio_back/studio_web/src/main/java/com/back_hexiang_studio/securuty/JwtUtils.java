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

    // token过期时间，从配置文件读取（单位：毫秒）
    @Value("${jwt.expiration}")
    private long expiration;
    
    // 签发人，从配置文件读取
    @Value("${jwt.issuer}")
    private String issuer;

    //根据用户id生成Token
    public String generateToken(Long user_id){
        return Jwts.builder()
                .setSubject(user_id.toString())//用户id放到subject中
                .signWith(SignatureAlgorithm.HS512, secret) //使用HS512算法，secret作为密钥
                .setIssuer(issuer)//设置签发人
                .setIssuedAt(new Date())//设置签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration))//设置过期时间
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

    //  检查令牌是否有效
    public boolean isTokenValid(String token){
        try{
            //传入token ，利用 密钥解析 token
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            //如果解析成功，说明token没有过期
            return true;
        }catch (JwtException e){
            //如果解析失败，说明token过期
            return false;
        }
    }


}
