package com.back_hexiang_studio.dv.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentVo {

    private Long studentId;
    /**
     * 用户id，对应表中 user_id 字段，类型 bigint
     */

    private Long userId;
    /**
     * 入学年份，对应表中 grade_year 字段，类型 varchar(20)
     */


    private String gradeYear;
    /**
     * 专业，对应表中 majorClass 字段，类型 varchar(20)
     */
    private String name;


    private String majorClass;
    /**
     * 学号，对应表中 student_number 字段，类型 varchar(20)
     */

    private Long studentNumber;
    /**
     * 培训方向，对应表中 direction_id 字段，类型 bigint
     */

    private Long directionId;
    /**
     * 辅导员，对应表中 counselor 字段，类型 varchar(20)
     */
    private String directionName;

    private String counselor;
    /**
     * 宿舍-楼号，对应表中 dormitory 字段，类型 varchar(255)
     */

    private String dormitory;
    /**
     * 分数，对应表中 score 字段，类型 varchar(255)
     */

    private String score;

    private String joinTime;
}
