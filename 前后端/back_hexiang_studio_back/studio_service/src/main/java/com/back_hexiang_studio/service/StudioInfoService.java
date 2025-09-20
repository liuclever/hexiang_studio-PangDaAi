package com.back_hexiang_studio.service;

import com.back_hexiang_studio.dv.vo.StudioInfoVo;
import com.back_hexiang_studio.entity.StudioInfo;

public interface StudioInfoService {
    
    /**
     * 获取工作室信息
     * @return 工作室信息
     */
    StudioInfoVo getStudioInfo();
    
    /**
     * 更新工作室信息
     * @param studioInfo 工作室信息
     */
    void updateStudioInfo(StudioInfo studioInfo);
} 