package com.back_hexiang_studio.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.StrUtil;
import com.back_hexiang_studio.cache.RedisCache;
import com.back_hexiang_studio.constants.CacheConstants;
import com.back_hexiang_studio.constants.CaptchaConstants;
import com.back_hexiang_studio.constants.SecurityConstants;
import com.back_hexiang_studio.dv.vo.CaptchaVo;
import com.back_hexiang_studio.result.Result;
import com.back_hexiang_studio.service.CaptchaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;



    //  验证码服务实现类
    @Override
    public Result<CaptchaVo> generateCaptcha() {
        // 生成唯一会话ID
        String sessionId = UUID.randomUUID().toString();
        log.info("开始生成验证码 - SessionId: {}", sessionId);

        try {
            CaptchaVo captchaVo = createCaptchaImage(sessionId);

            log.info("验证码生成成功 - SessionId: {}", sessionId);
            return Result.success(captchaVo);

        } catch (Exception e) {
            log.error("验证码生成失败 - SessionId: {}, Error: {}", sessionId, e.getMessage(), e);
            return Result.error("验证码生成失败，请稍后重试");
        }
    }


    //  验证码校验服务实现类
    @Override
    public Result<Boolean> validateCaptcha(String sessionId, String captcha) {
        log.info("开始验证验证码,id:{}",sessionId);
        //第一步：参数验证
        if(StrUtil.isBlank(sessionId)){
            log.warn("验证码验证失败：SessionId为空");
            return Result.error("会话已失效，请重新获取验证码");
        }

        if (StrUtil.isBlank(captcha)){
            log.warn("验证码验证失败：验证码为空");
            return Result.error("验证码不能为空");
        }

        //第二步：检查验证次数
        try{

            String failContKey="captcha_fail"+ sessionId;
            Integer failCount= redisCache.getCacheObject(failContKey);
            if(failCount!=null && failCount>= SecurityConstants.MAX_LOGIN_FAIL_ATTEMPTS){
                log.warn("验证码验证失败：验证次数过多");
                return Result.error("验证码验证失败：验证次数过多");
            }
            //第三步：检查验证码
            String captchaKey = CacheConstants.captchaKey(sessionId);
            String correctCode = redisCache.getCacheObject(captchaKey);

            //第四步：验证码是否过期

         if(StrUtil.isBlank(correctCode)){
             log.warn("验证码验证失败：验证码已过期");
             //清理次数
             redisCache.deleteObject(captchaKey);
             return Result.error("验证码已过期");
         }

         //第五步：验证码是否正确
            String userInput=captcha.trim().toLowerCase();
         String correctInput=correctCode.trim().toLowerCase();

         if(userInput.equals(correctInput)){
             log.info("验证码验证成功");
             //删除验证码缓存
             redisCache.deleteObject(captchaKey);
             //清理次数
             redisCache.deleteObject(failContKey);
             return Result.success(true,"验证码验证成功");
         }else {
             log.warn("验证码验证失败");

             //次数+1
             failCount=(failCount==null)?1:failCount+1;
             redisCache.setCacheObject(failContKey,failCount,SecurityConstants.ACCOUNT_LOCK_DURATION);

             //次数提示
             if (failCount>=SecurityConstants.MAX_LOGIN_FAIL_ATTEMPTS){
                return Result.error("验证码验证失败，请重新输入");
             }else{
                 return Result.error(String.format("验证码验证失败，请重新输入，剩余次数：%d",SecurityConstants.MAX_LOGIN_FAIL_ATTEMPTS-failCount));
             }
         }

        }catch (Exception e){
            log.error("验证码验证异常 -sessionId{},error{}", sessionId,e.getMessage(),e);
            return Result.error("系统繁忙，请稍后尝试");
        }
    }

    @Override
    public Result<CaptchaVo> refreshCaptcha(String sessionId) {
        log.info("开始刷新验证码 - SessionId: {}", sessionId);

        // ️ 参数有效性检查
        if (StrUtil.isBlank(sessionId)) {
            log.warn("验证码刷新失败：SessionId为空");
            return Result.error("会话无效，请重新获取验证码");
        }

        try {
            //  检查原验证码是否存在
            String cacheKey = CacheConstants.captchaKey(sessionId);
            String oldCode = redisCache.getCacheObject(cacheKey);

            if (StrUtil.isBlank(oldCode)) {
                log.info("原验证码已过期，重新生成 - SessionId: {}", sessionId);
            } else {
                log.info("刷新现有验证码 - SessionId: {}", sessionId);
            }

            //  生成新的验证码
            CaptchaVo newCaptchaVo = createCaptchaImage(sessionId);

            //  重置失败计数，给用户新的验证机会
            String failCountKey = "captcha_fail:" + sessionId;
            redisCache.deleteObject(failCountKey);

            log.info("验证码刷新成功 - SessionId: {}, 失败计数已重置", sessionId);

            //  返回成功结果
            return Result.success(newCaptchaVo);

        } catch (Exception e) {
            log.error("验证码刷新异常 - SessionId: {}, Error: {}", sessionId, e.getMessage(), e);
            return Result.error("刷新失败，请稍后重试");
        }
    }


    //  清理过期验证码服务实现类
    @Override
    public Result<Integer> cleanExpiredCaptcha() {
        log.info("开始清理过期验证码缓存");
        long startTime = System.currentTimeMillis();

        int totalCleaned = 0;
        int captchaCleaned = 0;
        int failCountCleaned = 0;

        try {
            // 🔍 第1步：获取所有验证码相关的Key
            Set<String> captchaKeys = redisTemplate.keys(CacheConstants.CAPTCHA_PATTERN);
            Set<String> failCountKeys = redisTemplate.keys(CacheConstants.CAPTCHA_FAIL_PATTERN);

            log.info("扫描到验证码Key数量: {}, 失败计数Key数量: {}",
                    captchaKeys != null ? captchaKeys.size() : 0,
                    failCountKeys != null ? failCountKeys.size() : 0);

            // 🧹 第2步：清理过期的验证码Key
            if (captchaKeys != null && !captchaKeys.isEmpty()) {
                captchaCleaned = cleanExpiredKeys(captchaKeys, "验证码");
            }

            // 🧹 第3步：清理孤立的失败计数Key（对应的验证码已过期）
            if (failCountKeys != null && !failCountKeys.isEmpty()) {
                failCountCleaned = cleanOrphanedFailCountKeys(failCountKeys);
            }

            totalCleaned = captchaCleaned + failCountCleaned;
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;

            log.info("过期验证码清理完成 - 总清理数量: {}, 验证码: {}, 失败计数: {}, 耗时: {}ms",
                    totalCleaned, captchaCleaned, failCountCleaned, costTime);

            return Result.success(totalCleaned,
                    String.format("清理完成，共清理%d个过期缓存，耗时%dms", totalCleaned, costTime));

        } catch (Exception e) {
            log.error("清理过期验证码异常: {}", e.getMessage(), e);
            return Result.error("清理任务执行失败: " + e.getMessage());
        }
    }




    /**
     * 创建验证码图片的通用方法
     * 企业级设计：代码复用，减少重复逻辑
     *
     * @param sessionId 会话ID
     * @return 验证码响应对象
     */
    private CaptchaVo createCaptchaImage(String sessionId) {
        //  创建验证码图片
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                CaptchaConstants.IMAGE_WIDTH,     // 120像素宽
                CaptchaConstants.IMAGE_HEIGHT,    // 40像素高
                CaptchaConstants.CODE_LENGTH,     // 4位验证码
                9                              // 5条干扰线
        );

        //  获取验证码内容
        String code = captcha.getCode();
        String imageBase64 = captcha.getImageBase64();

        log.debug("生成验证码内容 - SessionId: {}, Code: {}", sessionId, code);

        //  存储验证码到Redis（5分钟过期）
        String cacheKey = CacheConstants.captchaKey(sessionId);
        redisCache.setCacheObject(cacheKey, code, CaptchaConstants.EXPIRE_SECONDS);

        log.debug("验证码已存储到缓存 - Key: {}, ExpireSeconds: {}",
                cacheKey, CaptchaConstants.EXPIRE_SECONDS);

        //  构建返回对象
        return CaptchaVo.builder()
                .sessionId(sessionId)
                .imageBase64(CaptchaConstants.BASE64_PREFIX + imageBase64)
                .expireSeconds(CaptchaConstants.EXPIRE_SECONDS)
                .createTime(System.currentTimeMillis())
                .hint(CaptchaConstants.DEFAULT_HINT)
                .width(CaptchaConstants.IMAGE_WIDTH)
                .height(CaptchaConstants.IMAGE_HEIGHT)
                .build();
    }
    /**
     * 清理过期的Key集合
     *
     * @param keys Key集合
     * @param keyType Key类型描述（用于日志）
     * @return 清理数量
     */
    private int cleanExpiredKeys(Set<String> keys, String keyType) {
        int cleanedCount = 0;

        for (String key : keys) {
            try {
                // 检查Key是否存在（可能已自然过期）
                if (!redisTemplate.hasKey(key)) {
                    cleanedCount++;
                    continue;
                }

                // 检查剩余TTL
                Long ttl = redisTemplate.getExpire(key);
                if (ttl != null && ttl <= 0) {
                    // TTL <= 0 表示已过期但未被清理
                    redisTemplate.delete(key);
                    cleanedCount++;
                    log.debug("清理过期{}Key: {}", keyType, key);
                }

            } catch (Exception e) {
                log.warn("清理{}Key失败: {}, Error: {}", keyType, key, e.getMessage());
                // 继续处理其他Key，不因单个失败中断整体清理
            }
        }

        return cleanedCount;
    }

    /**
     * 清理孤立的失败计数Key
     * （对应的验证码已过期，但失败计数还存在）
     *
     * @param failCountKeys 失败计数Key集合
     * @return 清理数量
     */
    private int cleanOrphanedFailCountKeys(Set<String> failCountKeys) {
        int cleanedCount = 0;

        for (String failKey : failCountKeys) {
            try {
                // 提取SessionId：captcha_fail:sessionId -> sessionId
                String sessionId = failKey.replace("captcha_fail:", "");
                String captchaKey = CacheConstants.captchaKey(sessionId);

                // 检查对应的验证码是否还存在
                if (!redisTemplate.hasKey(captchaKey)) {
                    // 验证码已不存在，清理孤立的失败计数
                    redisTemplate.delete(failKey);
                    cleanedCount++;
                    log.debug("清理孤立的失败计数Key: {}", failKey);
                }

            } catch (Exception e) {
                log.warn("清理失败计数Key失败: {}, Error: {}", failKey, e.getMessage());
            }
        }

        return cleanedCount;
    }
}
