package com.back_hexiang_studio.utils;

import org.springframework.stereotype.Component;

/**
 * 位置验证
 * 用于验证签到位置是否在有效范围内
 */
@Component
public class LocationVerificationService {
    
    /**
     * 验证签到位置是否在有效范围内
     * @param checkInLat 签到纬度
     * @param checkInLng 签到经度
     * @param targetLat 目标纬度
     * @param targetLng 目标经度
     * @param radius 有效半径(米)
     * @return 是否在有效范围内
     */
    public boolean isLocationValid(Double checkInLat, Double checkInLng, 
                                  Double targetLat, Double targetLng, Integer radius) {
        if (checkInLat == null || checkInLng == null || 
            targetLat == null || targetLng == null || radius == null) {
            return false;
        }
        
        // 计算两点之间的距离
        double distance = calculateDistance(checkInLat, checkInLng, targetLat, targetLng);
        
        // 判断是否在有效半径内
        return distance <= radius;
    }
    
    /**
     * 计算两点之间的距离(米)，使用Haversine公式
     * 该公式考虑了地球曲率，适用于较大距离的计算
     * @param lat1 第一点纬度
     * @param lng1 第一点经度
     * @param lat2 第二点纬度
     * @param lng2 第二点经度
     * @return 距离(米)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // 地球半径（千米）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // 转换为米
        return R * c * 1000;
    }
}