package com.back_hexiang_studio.enumeration;

import java.util.Arrays;
import java.util.List;

public enum AttendanceStatus {
    pending,//"待签到"
    present,//已签到"
    late,//迟到"
    absent,//"缺席"
    leave;//"请假"



    public static boolean isValid(String value) {
        for (AttendanceStatus status : AttendanceStatus.values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
