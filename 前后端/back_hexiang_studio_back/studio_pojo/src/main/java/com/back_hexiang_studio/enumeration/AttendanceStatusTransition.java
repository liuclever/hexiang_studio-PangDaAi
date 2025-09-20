package com.back_hexiang_studio.enumeration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考勤状态转换规则
 * 定义哪些状态转换是被允许的
 */
public enum AttendanceStatusTransition {
    
    // 允许的状态转换规则
    PENDING_TO_PRESENT(AttendanceStatus.pending, AttendanceStatus.present, "正常签到"),
    PENDING_TO_LATE(AttendanceStatus.pending, AttendanceStatus.late, "迟到签到"), 
    PENDING_TO_ABSENT(AttendanceStatus.pending, AttendanceStatus.absent, "定时任务标记缺勤"),
    PENDING_TO_LEAVE(AttendanceStatus.pending, AttendanceStatus.leave, "请假审批通过"),
    
    ABSENT_TO_LEAVE(AttendanceStatus.absent, AttendanceStatus.leave, "补请假审批通过"),
    
    // 不允许的转换会在校验时被拒绝
    // present/late -> leave 不被允许
    // present/late -> absent 不被允许
    // leave -> 任何其他状态 不被允许
    ;
    
    private final AttendanceStatus fromStatus;
    private final AttendanceStatus toStatus;
    private final String description;
    
    AttendanceStatusTransition(AttendanceStatus fromStatus, AttendanceStatus toStatus, String description) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.description = description;
    }
    
    /**
     * 检查状态转换是否被允许
     */
    public static boolean isTransitionAllowed(AttendanceStatus from, AttendanceStatus to) {
        if (from == to) {
            return true; // 同状态转换总是允许的
        }
        
        return Arrays.stream(values())
                .anyMatch(transition -> transition.fromStatus == from && transition.toStatus == to);
    }
    
    /**
     * 获取指定状态可以转换到的所有状态
     */
    public static Set<AttendanceStatus> getAllowedTransitions(AttendanceStatus from) {
        return Arrays.stream(values())
                .filter(transition -> transition.fromStatus == from)
                .map(transition -> transition.toStatus)
                .collect(Collectors.toSet());
    }
    
    /**
     * 获取转换描述
     */
    public static String getTransitionDescription(AttendanceStatus from, AttendanceStatus to) {
        return Arrays.stream(values())
                .filter(transition -> transition.fromStatus == from && transition.toStatus == to)
                .map(transition -> transition.description)
                .findFirst()
                .orElse("不允许的状态转换");
    }
    
    // Getters
    public AttendanceStatus getFromStatus() { return fromStatus; }
    public AttendanceStatus getToStatus() { return toStatus; }
    public String getDescription() { return description; }
} 