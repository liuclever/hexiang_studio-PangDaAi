package com.back_hexiang_studio.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期时间工具类
 * 用于处理String和LocalDateTime之间的转换
 */
public class DateTimeUtils {

    // 常用日期格式
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 将LocalDateTime转为String
     * @param dateTime LocalDateTime对象
     * @return 格式化后的日期时间字符串，如果输入为null则返回null
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return formatDateTime(dateTime, DATETIME_FORMAT);
    }

    /**
     * 将LocalDateTime转为String，使用指定格式
     * @param dateTime LocalDateTime对象
     * @param pattern 日期格式
     * @return 格式化后的日期时间字符串，如果输入为null则返回null
     */
    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }

    /**
     * 将LocalDate转为String
     * @param date LocalDate对象
     * @return 格式化后的日期字符串，如果输入为null则返回null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return date.format(formatter);
    }

    /**
     * 将String转为LocalDateTime
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime对象，如果输入为null或空或格式错误则返回null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        // 尝试多种格式解析
        try {
            // 1. 尝试ISO LocalDateTime格式 (例如 "2025-08-01T17:56:49")
            if (dateTimeStr.contains("T") && !dateTimeStr.contains("Z") && !dateTimeStr.contains("+")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            // 2. 尝试ISO格式带时区 (例如 "2025-07-08T10:00:00.000Z")
            if (dateTimeStr.contains("Z") || dateTimeStr.contains("+")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            }
            
            // 3. 尝试标准格式 (例如 "2025-08-01 17:56:49")
            if (dateTimeStr.contains(" ")) {
                return parseDateTime(dateTimeStr, DATETIME_FORMAT);
            }
            
            // 4. 直接尝试ISO格式
            return LocalDateTime.parse(dateTimeStr);
            
        } catch (DateTimeParseException e) {
            // 如果以上格式都解析失败，再尝试自定义的默认格式
            try {
                return parseDateTime(dateTimeStr, DATETIME_FORMAT);
            } catch (DateTimeParseException e2) {
                System.err.println("无法解析日期时间字符串: " + dateTimeStr + ", error: " + e2.getMessage());
                return null;
            }
        }
    }

    /**
     * 将String转为LocalDateTime，使用指定格式
     * @param dateTimeStr 日期时间字符串
     * @param pattern 日期格式
     * @return LocalDateTime对象，如果输入为null或空或格式错误则返回null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            // 记录日志
            System.err.println("日期解析错误: " + dateTimeStr + ", 格式: " + pattern);
            return null;
        }
    }

    /**
     * 将String转为LocalDate
     *
     * @param dateStr 日期字符串
     * @return LocalDate对象，如果输入为null或空或格式错误则返回null
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            return LocalDate.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            // 记录日志
            System.err.println("日期解析错误: " + dateStr);
            return null;
        }
    }

    /**
     * 将String转为java.util.Date
     *
     * @param dateStr 日期字符串
     * @return Date对象，如果输入为null或空或格式错误则返回null
     */
    public static java.util.Date parseToDate(String dateStr) {
        LocalDate localDate = parseDate(dateStr);
        if (localDate == null) {
            return null;
        }
        return java.sql.Date.valueOf(localDate);
    }

    /**
     * 将日期字符串转换为LocalDateTime，设置时间为当天的开始(00:00:00)
     * @param dateStr 日期字符串(yyyy-MM-dd)
     * @return LocalDateTime对象
     */
    public static LocalDateTime parseDateStartOfDay(String dateStr) {
        LocalDate date = parseDate(dateStr);
        return date != null ? date.atStartOfDay() : null;
    }

    /**
     * 将日期字符串转换为LocalDateTime，设置时间为当天的结束(23:59:59.999999999)
     * @param dateStr 日期字符串(yyyy-MM-dd)
     * @return LocalDateTime对象
     */
    public static LocalDateTime parseDateEndOfDay(String dateStr) {
        LocalDate date = parseDate(dateStr);
        return date != null ? date.atTime(LocalTime.MAX) : null;
    }

    /**
     * 检查字符串是否为有效的日期时间格式
     * @param dateTimeStr 日期时间字符串
     * @param pattern 日期格式
     * @return 是否有效
     */
    public static boolean isValidDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return false;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDateTime.parse(dateTimeStr, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}