package com.back_hexiang_studio.constants;

/**
 * 验证码配置常量
 *
 * @author wenhan
 * @since 1.0.0
 */
public class CaptchaConstants {

    /**
     * 验证码长度
     */
    public static final int CODE_LENGTH = 4;

    /**
     * 验证码有效期（秒）- 5分钟
     */
    public static final int EXPIRE_SECONDS = 5 * 60;

    /**
     * 验证码图片宽度（像素）
     */
    public static final int IMAGE_WIDTH = 120;

    /**
     * 验证码图片高度（像素）
     */
    public static final int IMAGE_HEIGHT = 40;

    /**
     * 验证码字符集（纯数字，避免字母混淆）
     */
    public static final String CODE_CHARS = "0123456789";

    /**
     * 验证码图片格式
     */
    public static final String IMAGE_FORMAT = "png";

    /**
     * Base64前缀
     */
    public static final String BASE64_PREFIX = "data:image/png;base64,";

    /**
     * 默认提示信息
     */
    public static final String DEFAULT_HINT = "请输入图中4位数字验证码";
}