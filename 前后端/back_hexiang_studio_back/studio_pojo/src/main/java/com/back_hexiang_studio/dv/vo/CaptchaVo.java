package com.back_hexiang_studio.dv.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应VO - 企业级设计
 *
 * 设计原则：
 * 1. 包含完整的验证码信息
 * 2. 前端渲染所需的所有数据
 * 3. 支持链式调用和建造者模式
 * 4. 清晰的字段注释和验证规则
 *
 * @author 何湘工作室
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVo {

    /**
     * 验证码图片Base64编码
     * 格式：data:image/png;base64,xxxxx
     */
    private String imageBase64;

    /**
     * 验证码会话ID（前端需要在验证时回传）
     * UUID格式，确保全局唯一性
     */
    private String sessionId;

    /**
     * 验证码有效期（秒）
     * 默认300秒（5分钟）
     */
    private Integer expireSeconds;

    /**
     * 验证码创建时间戳（毫秒）
     * 用于前端显示倒计时
     */
    private Long createTime;

    /**
     * 用户提示信息
     * 支持国际化
     */
    private String hint;

    /**
     * 验证码图片宽度（像素）
     */
    private Integer width;

    /**
     * 验证码图片高度（像素）
     */
    private Integer height;
}