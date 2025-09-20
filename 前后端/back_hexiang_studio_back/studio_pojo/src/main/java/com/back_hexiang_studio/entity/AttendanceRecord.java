package com.back_hexiang_studio.entity;

import com.back_hexiang_studio.enumeration.AttendanceStatus;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AttendanceRecord {
    private Long recordId;        // 记录ID
    private Long planId;          // 考勤计划ID
    private Long studentId;       // 学生ID
    private AttendanceStatus status;        // 签到状态：present-已签到,late-迟到,absent-缺勤,leave-请假
    private LocalDateTime signInTime; // 签到时间
    private String location;      // 签到位置
    private Double locationLat;   // 签到纬度
    private Double locationLng;   // 签到经度
    private String remark;        // 备注
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Long updateUser;      // 最后修改人ID
} 