package com.back_hexiang_studio.enumeration;

import lombok.Getter;

/**
 * 文件类型枚举
 * <p>
 * 用于定义不同业务模块的文件存储子路径，确保文件管理的规范性和可扩展性。
 * 每个枚举常量都包含一个路径和一个描述。
 * </p>
 *
 * @author Gemini
 * @since 2024-07-12
 */
@Getter
public enum FileType {

    /**
     * 公告模块的配图
     */
    NOTICE_IMAGE("notice/image", "公告图片"),

    /**
     * 公告模块的附件
     */
    NOTICE_ATTACHMENT("notice/attachment", "公告附件"),

    /**
     * 课程模块的资料
     */
    COURSE_MATERIAL("course/material", "课程资料"),

    /**
     * 课程封面图片
     * 路径：/course/cover/{YYYY}/{MM}/{DD}/{UUID}.{ext}
     */
    COURSE_COVER("course/cover", "课程封面"),

    /**
     * 任务模块中，由用户提交的文件
     */
    TASK_SUBMISSION("task/submission", "任务提交"),

    /**
     * 任务模块中，由管理员上传的附件
     */
    TASK_ATTACHMENT("task/attachment", "任务附件"),

    /**
     * 考勤模块的打卡证据（如截图）
     */
    ATTENDANCE_EVIDENCE("attendance/evidence", "考勤证据"),

    /**
     * 请假申请的附件文件（如病假证明、请假条等）
     */
    LEAVE_ATTACHMENT("leave/attachment", "请假申请附件"),

    /**
     * 学生用户的头像
     */
    AVATAR_STUDENT("avatar/student", "学生头像"),

    /**
     * 教师用户的头像
     */
    AVATAR_TEACHER("avatar/teacher", "教师头像"),

    /**
     * 管理员用户的头像
     */
    AVATAR_ADMIN("avatar/admin", "管理员头像"),

    /**
     * 用户荣誉证明（如奖状图片）
     */
    USER_HONOR("user/honor", "用户荣誉证明"),
    
    /**
     * 用户证书证明（如技能证书图片）
     */
    USER_CERTIFICATE("user/certificate", "用户证书"),

    /**
     * 用于存放临时或未分类文件的目录
     */
    TEMP("temp", "临时文件");

    /**
     * 文件存储的相对子路径 (例如: "notice/image")
     */
    private final String path;

    /**
     * 文件类型的中文描述 (例如: "公告图片")
     */
    private final String description;



    /**
     * 构造函数
     *
     * @param path        相对子路径
     * @param description 中文描述
     */
    FileType(String path, String description) {
        this.path = path;
        this.description = description;
    }

} 